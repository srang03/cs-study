# Claude Code 설정과 권한

설정 scope 체계, settings.json 구조, 권한 모드, 권한 규칙 문법을 다룬다.

---

## 1. 설정 Scope

| Scope | 위치 | 적용 대상 | 팀 공유 |
|-------|------|----------|---------|
| Managed | 서버/MDM/시스템 파일 | 조직 전체 | IT 배포 |
| User | `~/.claude/settings.json` | 모든 프로젝트 | 개인 |
| Project | `.claude/settings.json` | 현재 프로젝트 | git 커밋 |
| Local | `.claude/settings.local.json` | 현재 프로젝트 | 개인 (gitignore) |

### 우선순위

```
Managed (최우선) > CLI 인자 > Local > Project > User (최하위)
```

- Managed는 어떤 레벨에서도 오버라이드할 수 없다
- 배열 설정은 scope 간에 **병합(merge)** 된다 (교체가 아님)
- deny가 어느 레벨에서든 설정되면 다른 레벨에서 allow할 수 없다

### Scope별 용도

| Scope | 적합한 용도 |
|-------|-----------|
| Managed | 보안 정책, 컴플라이언스, 조직 표준 |
| User | 개인 선호 (테마, 에디터), 전역 API 키 |
| Project | 팀 공유 설정 (권한, 훅, MCP 서버) |
| Local | 개인 오버라이드, 설정 테스트, 머신별 설정 |

### 기능별 Scope 매핑

| 기능 | User | Project | Local |
|------|------|---------|-------|
| Settings | `~/.claude/settings.json` | `.claude/settings.json` | `.claude/settings.local.json` |
| Subagents | `~/.claude/agents/` | `.claude/agents/` | — |
| MCP 서버 | `~/.claude.json` | `.mcp.json` | `~/.claude.json` (프로젝트별) |
| Plugins | `~/.claude/settings.json` | `.claude/settings.json` | `.claude/settings.local.json` |
| CLAUDE.md | `~/.claude/CLAUDE.md` | `CLAUDE.md` 또는 `.claude/CLAUDE.md` | — |

---

## 2. settings.json 주요 설정

### 기본 설정

| 키 | 설명 | 예시 |
|----|------|------|
| `model` | 기본 모델 오버라이드 | `"claude-sonnet-4-6"` |
| `availableModels` | 선택 가능 모델 제한 | `["sonnet", "haiku"]` |
| `effortLevel` | 추론 깊이 (`low`, `medium`, `high`) | `"medium"` |
| `language` | 응답 언어 | `"korean"` |
| `env` | 환경변수 설정 | `{"FOO": "bar"}` |

### 워크플로우 설정

| 키 | 설명 | 예시 |
|----|------|------|
| `permissions` | 권한 규칙 (아래 상세) | — |
| `hooks` | 생명주기 이벤트 자동화 | [hooks.md](claude-code-hooks.md) 참조 |
| `agent` | 메인 스레드를 특정 에이전트로 실행 | `"code-reviewer"` |
| `includeGitInstructions` | 기본 git 워크플로우 지침 포함 여부 | `false` |

### 표시 설정

| 키 | 설명 | 예시 |
|----|------|------|
| `statusLine` | 커스텀 상태줄 | `{"type": "command", "command": "..."}` |
| `outputStyle` | 출력 스타일 | `"Explanatory"` |
| `spinnerVerbs` | 스피너 동사 커스터마이징 | `{"mode": "append", "verbs": ["Pondering"]}` |
| `prefersReducedMotion` | UI 애니메이션 줄이기 | `true` |

### Worktree 설정

| 키 | 설명 | 예시 |
|----|------|------|
| `worktree.symlinkDirectories` | 심링크할 디렉토리 (디스크 절약) | `["node_modules", ".cache"]` |
| `worktree.sparsePaths` | sparse-checkout 경로 (대형 모노레포) | `["packages/my-app"]` |

### 기타 설정

| 키 | 설명 |
|----|------|
| `cleanupPeriodDays` | 비활성 세션 삭제 기간 (기본 30일). `0`이면 세션 영속화 비활성화 |
| `attribution` | git 커밋/PR 어트리뷰션 커스터마이징 |
| `autoUpdatesChannel` | 업데이트 채널 (`stable` 또는 `latest`) |
| `plansDirectory` | 플랜 파일 저장 위치 (기본 `~/.claude/plans`) |
| `fileSuggestion` | `@` 파일 자동완성 커스텀 명령 |

### JSON 스키마 지원

```json
{
  "$schema": "https://json.schemastore.org/claude-code-settings.json"
}
```

VS Code 등에서 자동완성과 인라인 검증이 활성화된다.

---

## 3. 권한 시스템

### 도구별 기본 동작

| 도구 유형 | 예시 | 승인 필요 |
|----------|------|----------|
| 읽기 전용 | 파일 읽기, Grep | 불필요 |
| Bash 명령 | 셸 실행 | 필요 (영구 허용 가능) |
| 파일 수정 | Edit, Write | 필요 (세션 종료까지) |

### 규칙 평가 순서

**deny → ask → allow** — 첫 번째 매칭 규칙이 적용된다. deny가 항상 우선한다.

### 관리

`/permissions` 명령으로 모든 권한 규칙과 소스를 확인할 수 있다.

---

## 4. 권한 모드

| 모드 | 설명 |
|------|------|
| `default` | 도구 첫 사용 시 승인 요청 |
| `acceptEdits` | 파일 편집 자동 승인, 명령은 승인 필요 |
| `plan` | 분석만 가능, 파일 수정/명령 실행 불가 |
| `dontAsk` | `/permissions`이나 `permissions.allow`에 사전 승인된 도구만 실행. 나머지 자동 거부 |
| `bypassPermissions` | 승인 프롬프트 생략. `.git`, `.claude`, `.vscode`, `.idea` 쓰기만 프롬프트 표시 (단, `.claude/commands`, `.claude/agents`, `.claude/skills` 쓰기는 면제) |

설정 방법:

```json
{
  "permissions": {
    "defaultMode": "acceptEdits"
  }
}
```

> `bypassPermissions`는 격리된 환경(컨테이너, VM)에서만 사용한다. 관리자는 `disableBypassPermissionsMode: "disable"`로 차단할 수 있다.

---

## 5. 권한 규칙 문법

규칙 형식: `Tool` 또는 `Tool(specifier)`

### 전체 매칭

| 규칙 | 효과 |
|------|------|
| `Bash` | 모든 Bash 명령 매칭 |
| `WebFetch` | 모든 웹 요청 매칭 |
| `Read` | 모든 파일 읽기 매칭 |

### 와일드카드

`*`는 glob 패턴으로 동작한다. 명령 어디에든 사용 가능하다.

```json
{
  "permissions": {
    "allow": [
      "Bash(npm run *)",
      "Bash(git commit *)",
      "Bash(* --version)"
    ],
    "ask": [
      "Bash(git push *)"
    ],
    "deny": [
      "Bash(curl *)",
      "Bash(wget *)"
    ]
  }
}
```

- `allow`: 승인 없이 실행
- `ask`: 매번 확인 프롬프트 표시
- `deny`: 실행 차단

- `Bash(ls *)`: `ls -la` 매칭, `lsof` 미매칭 (공백이 단어 경계 역할)
- `Bash(ls*)`: `ls -la`와 `lsof` 모두 매칭

> 복합 명령(`&&`) 승인 시 각 서브 명령별로 규칙이 분리 저장된다 (최대 5개). `Bash(safe-cmd *)`는 `safe-cmd && other-cmd`에 권한을 부여하지 않는다.

> **Bash URL 패턴 주의**: `Bash(curl http://github.com/ *)`처럼 URL을 제약하는 패턴은 취약하다. 옵션 순서 변경(`curl -X GET http://...`), 프로토콜 차이(`https`), 리다이렉트(`curl -L http://bit.ly/...`), 변수 치환(`curl $URL`) 등으로 우회된다. URL 필터링이 필요하면 Bash에서 `curl`/`wget`을 deny하고 `WebFetch(domain:github.com)`을 사용하거나, PreToolUse 훅으로 검증한다.

### Read / Edit 경로 패턴

gitignore 스펙을 따른다.

| 패턴 | 의미 | 예시 |
|------|------|------|
| `//path` | 파일시스템 루트 절대 경로 | `Read(//Users/alice/secrets/**)` |
| `~/path` | 홈 디렉토리 기준 | `Read(~/Documents/*.pdf)` |
| `/path` | 프로젝트 루트 기준 | `Edit(/src/**/*.ts)` |
| `path` 또는 `./path` | 현재 디렉토리 기준 | `Read(*.env)` |

- `*`: 단일 디렉토리 내 파일 매칭
- `**`: 재귀적 디렉토리 매칭

> Read/Edit deny 규칙은 Claude의 내장 도구에만 적용된다. `cat .env` 같은 Bash 서브프로세스는 차단하지 않는다. OS 레벨 차단이 필요하면 샌드박스를 사용한다.

### WebFetch

```
WebFetch(domain:example.com)
```

### MCP 도구

```
mcp__puppeteer                    # 서버의 모든 도구
mcp__puppeteer__*                 # 와일드카드 (동일)
mcp__puppeteer__puppeteer_navigate  # 특정 도구
```

### Agent (서브에이전트)

```
Agent(Explore)
Agent(my-custom-agent)
```

---

## 6. 민감 파일 보호

```json
{
  "permissions": {
    "deny": [
      "Read(./.env)",
      "Read(./.env.*)",
      "Read(./secrets/**)",
      "Read(./config/credentials.json)"
    ]
  }
}
```

---

## 7. 작업 디렉토리

기본적으로 Claude는 실행된 디렉토리에만 접근한다. 확장 방법:

| 방법 | 시점 |
|------|------|
| `--add-dir <path>` | 시작 시 CLI 인자 |
| `/add-dir` | 세션 중 명령 |
| `additionalDirectories` | settings.json 영구 설정 |

---

## 8. Managed 전용 설정

조직 관리자만 사용 가능한 설정이다.

| 설정 | 설명 |
|------|------|
| `disableBypassPermissionsMode` | `bypassPermissions` 모드와 `--dangerously-skip-permissions` 플래그 차단 |
| `allowManagedPermissionRulesOnly` | 사용자/프로젝트 권한 규칙 차단. Managed 규칙만 적용 |
| `allowManagedHooksOnly` | 사용자/프로젝트/플러그인 훅 차단. Managed와 SDK 훅만 허용 |
| `allowManagedMcpServersOnly` | Managed `allowedMcpServers`만 적용 |
| `channelsEnabled` | 채널 메시지 전달 허용 (Team/Enterprise) |
| `allow_remote_sessions` | 원격 세션 허용 여부 (기본 `true`) |

### Managed 설정 파일 위치

| OS | 경로 |
|----|------|
| macOS | `/Library/Application Support/ClaudeCode/managed-settings.json` |
| Linux/WSL | `/etc/claude-code/managed-settings.json` |
| Windows | `C:\Program Files\ClaudeCode\managed-settings.json` |

---

## 9. 설정 확인

| 명령 | 용도 |
|------|------|
| `/config` | 설정 UI 열기 |
| `/status` | 활성 설정 소스 확인 |
| `/permissions` | 권한 규칙 목록과 소스 확인 |
