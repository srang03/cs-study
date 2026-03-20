# Claude Code MCP 연결 및 사용법

---

## 1. MCP란

MCP(Model Context Protocol)는 Claude Code가 외부 도구, API, 데이터 소스와 연결할 때 사용하는 개방형 표준이다.
GitHub, Notion, Sentry, 사내 API, 로컬 스크립트 같은 도구를 Claude Code의 도구로 연결할 수 있다.

---

## 2. 전송 방식

| 방식 | 용도 | 상태 |
|------|------|------|
| `http` | 원격 MCP 서버 | **권장** |
| `stdio` | 로컬 프로세스 실행 | 활성 |
| `sse` | 원격 SSE 스트리밍 | **deprecated** — HTTP 사용 권장 |

---

## 3. 연결 방법

기본 명령은 `claude mcp add`이다. 모든 옵션은 서버 이름 **앞에** 와야 한다.

### HTTP 서버

```bash
claude mcp add --transport http notion https://mcp.notion.com/mcp

# 인증 헤더 포함
claude mcp add --transport http secure-api https://api.example.com/mcp \
  --header "Authorization: Bearer YOUR_TOKEN"
```

### stdio 서버

```bash
claude mcp add --transport stdio my-server -- python server.py

# 환경변수 포함
claude mcp add --transport stdio --env API_KEY=secret my-server -- npx -y my-package
```

`--` 뒤에 실제 MCP 서버를 실행할 명령과 인자가 온다.

### JSON으로 등록

```bash
claude mcp add-json weather-api \
  '{"type":"http","url":"https://api.weather.com/mcp","headers":{"Authorization":"Bearer token"}}'
```

### 주요 플래그

| 플래그 | 용도 | 예시 |
|--------|------|------|
| `--transport` | 전송 방식 | `http`, `stdio`, `sse` |
| `--scope` | 저장 scope | `local`, `project`, `user` |
| `--env` | 환경변수 전달 | `--env KEY=value` |
| `--header` | HTTP 헤더 | `--header "Authorization: Bearer token"` |
| `--callback-port` | OAuth 콜백 포트 고정 | `--callback-port 8080` |
| `--client-id` | OAuth 클라이언트 ID | `--client-id your-id` |
| `--client-secret` | OAuth 클라이언트 시크릿 (대화형 입력) | `--client-secret` |

---

## 4. Scope와 저장 위치

| scope | 의미 | 저장 위치 | 공유 |
|-------|------|----------|------|
| `local` (기본값) | 현재 프로젝트, 개인 설정 | `~/.claude.json` (프로젝트 경로별) | 개인 |
| `project` | 팀 공유 프로젝트 설정 | 프로젝트 루트 `.mcp.json` | 팀 (git 포함) |
| `user` | 모든 프로젝트 전역 설정 | `~/.claude.json` (전역) | 개인 |
| `managed` | 조직 정책 배포 | 시스템 경로 `managed-mcp.json` | 조직 전체 |

### 우선순위

**local > project > user** — 동일 이름의 서버가 여러 scope에 있으면 상위 scope가 우선한다.

### Managed MCP 시스템 경로

- macOS: `/Library/Application Support/ClaudeCode/managed-mcp.json`
- Linux/WSL: `/etc/claude-code/managed-mcp.json`
- Windows: `C:\Program Files\ClaudeCode\managed-mcp.json`

---

## 5. .mcp.json 파일 형식

프로젝트 루트에 `.mcp.json`을 직접 작성하여 팀과 공유할 수 있다.

```json
{
  "mcpServers": {
    "http-server": {
      "type": "http",
      "url": "https://api.example.com/mcp",
      "headers": {
        "Authorization": "Bearer ${API_TOKEN}"
      }
    },
    "stdio-server": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "my-mcp-server"],
      "env": {
        "API_KEY": "${API_KEY}",
        "DEBUG": "${DEBUG:-false}"
      }
    }
  }
}
```

### 환경변수 확장

| 문법 | 동작 |
|------|------|
| `${VAR}` | 환경변수 VAR의 값. 미설정 시 오류 |
| `${VAR:-default}` | VAR이 설정되어 있으면 그 값, 아니면 default 사용 |

`url`, `command`, `args`, `env`, `headers` 필드에서 사용 가능하다.

---

## 6. Claude Desktop에서 가져오기

```bash
claude mcp add-from-claude-desktop
```

- 설정을 읽어와 등록하는 **import**이다 (자동 동기화가 아님)
- macOS와 WSL에서 동작한다
- Claude Desktop의 `claude_desktop_config.json`을 읽어 대화형으로 선택

---

## 7. 연결 확인과 인증

### CLI 명령

```bash
claude mcp list              # 전체 서버 목록
claude mcp get <name>        # 특정 서버 상세 정보
claude mcp remove <name>     # 서버 삭제
claude mcp reset-project-choices  # 프로젝트 서버 승인 초기화
claude mcp serve             # Claude Code를 MCP 서버로 제공
```

### /mcp 명령 (세션 내)

`/mcp`는 단순 목록 조회가 아니라 상태 확인과 인증 관리 진입점이다:
- 연결된 서버 상태 확인
- OAuth 인증 시작/해제
- 서버별 도구 수 확인

### 진단

```bash
claude doctor    # 설치/설정 전체 진단
```

---

## 8. OAuth 인증

### 자동 등록 (대부분의 경우)

```bash
claude mcp add --transport http sentry https://mcp.sentry.dev/mcp
```

서버가 OAuth를 요구하면 `/mcp`에서 브라우저 인증 흐름이 자동으로 시작된다. 토큰은 자동 저장/갱신된다.

### 사전 등록된 OAuth 앱

서버의 개발자 포털에서 OAuth 앱을 등록한 경우:

```bash
claude mcp add --transport http \
  --client-id your-client-id --client-secret --callback-port 8080 \
  my-server https://mcp.example.com/mcp
```

### 토큰 관리

- 토큰은 시스템 키체인에 안전하게 저장된다
- 자동 갱신
- `/mcp` → "Clear authentication"으로 토큰 삭제 가능

---

## 9. Tool Search

MCP 도구가 컨텍스트 윈도우의 10%를 초과하면 자동으로 활성화된다.
모든 도구를 미리 로딩하는 대신, 필요할 때만 검색하여 로딩한다.

```bash
# 임계치 조정 (5%)
ENABLE_TOOL_SEARCH=auto:5 claude

# 비활성화
ENABLE_TOOL_SEARCH=false claude
```

> Haiku 모델은 Tool Search를 지원하지 않는다 (모든 도구가 미리 로딩됨)

---

## 10. 주의사항

### Windows에서 npx 사용

네이티브 Windows에서는 `cmd /c` 래퍼가 필요하다 (WSL/macOS는 불필요):

```bash
claude mcp add --transport stdio my-server -- cmd /c npx -y @some/package
```

### 프로젝트 서버 승인

`.mcp.json`의 서버는 보안상 최초 사용 시 승인 절차를 거친다. 승인 초기화: `claude mcp reset-project-choices`

### 민감 정보

API 키를 `.mcp.json`에 하드코딩하지 않는다. `${API_KEY}` 환경변수 확장을 사용한다.

### 제3자 서버

공식 문서 기준 Anthropic은 제3자 MCP 서버의 정확성과 보안을 검증하지 않는다. 팀 공유(`project` scope) 시 특히 주의한다.

### 디버깅 환경변수

| 변수 | 용도 | 기본값 |
|------|------|--------|
| `MCP_TIMEOUT` | 서버 시작 타임아웃 (ms) | 30000 |
| `MAX_MCP_OUTPUT_TOKENS` | 도구 출력 최대 토큰 | 10000 |
| `ENABLE_TOOL_SEARCH` | Tool Search 설정 | `auto` |
