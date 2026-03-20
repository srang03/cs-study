# Claude Code MCP 연결 및 사용법

## 1. MCP란

- MCP(Model Context Protocol)는 Claude Code가 외부 도구, API, 데이터 소스와 연결할 때 사용하는 방식입니다.
- GitHub, Notion, Sentry, 사내 API, 로컬 스크립트 같은 도구를 Claude Code의 도구로 연결할 수 있습니다.
- 이 문서는 MCP 개념 설명보다 개발자가 실제로 연결하고, 검증하고, 운영할 때 필요한 내용에 집중합니다.

## 2. MCP 연결 방법

Claude Code에서 MCP 서버를 추가할 때 기본 명령은 `claude mcp add` 입니다.

### HTTP 서버 연결

- 원격 MCP 서버는 `http` 전송 방식을 우선 사용합니다.
- `sse` 는 여전히 지원되지만, 신규 구성에서는 `http` 를 우선 권장합니다.

```bash
claude mcp add --transport http notion https://mcp.notion.com/mcp
```

인증 헤더가 필요한 경우:

```bash
claude mcp add --transport http secure-api https://api.example.com/mcp --header "Authorization: Bearer YOUR_TOKEN"
```

### stdio 서버 연결

- `stdio` 는 로컬 프로세스를 실행해 MCP 서버를 붙이는 방식입니다.
- 로컬 스크립트나 직접 실행해야 하는 개발 도구에 적합합니다.

```bash
claude mcp add --transport stdio my-server -- python server.py
```

- `--transport`, `--env`, `--scope`, `--header` 같은 옵션은 서버 이름 앞에 와야 합니다.
- `--` 뒤에는 실제 MCP 서버를 실행할 명령과 인자가 옵니다.

### JSON으로 등록

- 이미 준비된 설정 JSON이 있다면 `claude mcp add-json` 으로 등록할 수 있습니다.
- 여러 환경에 같은 설정을 반복 적용할 때 유용합니다.

## 3. 설정 파일과 scope

MCP는 일반 Claude 설정과 별도 파일 체계를 사용합니다.

| scope | 의미 | 저장 위치 |
|------|------|-----------|
| `user` | 모든 프로젝트에서 사용하는 개인 전역 설정 | `~/.claude/settings.json` 또는 `~/.claude/mcp.json` |
| `project` | 팀이 공유하는 프로젝트 공용 설정 | 프로젝트 루트 `.mcp.json` 또는 `.claude/mcp.json` |
| `local` | 현재 프로젝트에서만 쓰는 개인 설정 | `.claude/settings.local.json` |

- 기본 scope 는 `local` 입니다.
- 오래된 문서의 `global` 은 현재 `user` 입니다.
- 오래된 문서에서 개인 프로젝트용으로 설명하던 예전 `project` 는 현재 `local` 로 바뀌었습니다.
- `project` scope 는 보통 버전 관리에 포함해 팀과 공유합니다.
- 조직 정책으로 고정 배포하는 경우 `managed-mcp.json` 을 사용할 수 있습니다.

## 4. Claude Desktop에서 가져오기

Claude Desktop에 이미 설정된 MCP 서버를 Claude Code로 가져오려면 아래 명령을 사용합니다.

```bash
claude mcp add-from-claude-desktop
```

- 이 기능은 설정을 읽어와 등록하는 import 입니다.
- 자동 동기화(sync)로 설명하면 부정확합니다.
- 공식 문서 기준으로 Claude Desktop chat app의 `claude_desktop_config.json` 기반 MCP와 Claude Code의 MCP 설정은 별도입니다.
- Claude Code에서 MCP를 쓰려면 `~/.claude.json` 또는 프로젝트의 `.mcp.json` 을 기준으로 관리해야 합니다.
- 공식 문서 기준 `add-from-claude-desktop` 는 `macOS` 와 `WSL` 에서 동작합니다.

## 5. 연결 확인과 인증

연결 후에는 CLI와 Claude Code 내부 명령을 함께 사용해 검증합니다.

```bash
claude mcp list
claude mcp get github
claude mcp remove github
```

Claude Code 내부 `/mcp` 명령으로 다음을 처리할 수 있습니다.

- 서버 상태 확인
- OAuth 인증 시작
- 인증 해제

추가 진단에 유용한 명령:

```bash
/status
claude doctor
```

중요한 점:

- `/mcp` 는 단순 목록 조회 명령이 아니라 상태 확인과 인증 관리 진입점입니다.
- 원격 MCP 서버가 OAuth 2.0 인증을 요구하는 경우 `/mcp` 에서 인증을 진행합니다.

## 6. Windows 및 실무 주의사항

### Windows에서 `npx` 기반 stdio 서버 연결

- 네이티브 Windows 환경에서는 `npx` 기반 로컬 MCP 서버가 `cmd /c` 래퍼를 요구할 수 있습니다.
- 공식 문서 기준 이 래퍼가 없으면 `"Connection closed"` 오류가 날 수 있습니다.

```bash
claude mcp add --transport stdio my-server -- cmd /c npx -y @some/package
```

### 프로젝트 공유 설정

- 프로젝트 공용 MCP는 `.mcp.json` 에 저장됩니다.
- Claude Code는 보안상 프로젝트 MCP 사용 전에 승인 절차를 거칩니다.
- 승인 상태를 다시 고르려면 `claude mcp reset-project-choices` 를 사용할 수 있습니다.

### 민감 정보 관리

- API 키나 토큰을 `.mcp.json` 에 직접 하드코딩하지 않는 것이 좋습니다.
- 공식 문서 기준 환경변수 확장을 사용해 분리할 수 있습니다.

### 제3자 MCP 서버 검토

- 제3자 MCP 서버는 정확성, 권한 범위, 보안 수준을 별도로 검토해야 합니다.
- 특히 팀 공유용 `project` scope 에 추가할 때는 더 신중해야 합니다.

## 7. 개발자 기준 정리

- MCP 추가의 기본은 `claude mcp add` 입니다.
- 신규 원격 연결은 `http` 를 우선 고려하고, `sse` 는 레거시 호환이 필요한 경우만 검토합니다.
- `/mcp` 는 도구 목록 조회보다 상태 확인과 OAuth 인증 관리에 더 가깝습니다.
- 설정 파일은 `~/.claude.json` 과 `.mcp.json` 을 구분해 이해해야 합니다.
- Claude Desktop chat app의 MCP와 Claude Code의 MCP는 별도 구성이며, 필요하면 `claude mcp add-from-claude-desktop` 로 가져옵니다.

---

공식 기준으로 더 자세한 옵션과 최신 변경 사항은 Claude Code MCP 문서를 함께 확인하는 것이 가장 안전합니다.