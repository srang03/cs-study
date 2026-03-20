# Claude Code Hooks

Hooks는 Claude Code 생명주기의 특정 시점에 자동 실행되는 사용자 정의 명령이다.
LLM의 판단에 의존하지 않고, 규칙 기반으로 항상 동일하게 동작하는 자동화를 제공한다.

---

## 1. 용도

- 코드 편집 후 자동 포맷팅
- 위험한 명령 차단 (rm -rf 등)
- 보호 파일 수정 방지
- 데스크톱 알림 전송
- 권한 자동 승인/거부
- 컨텍스트 압축 후 정보 재주입

---

## 2. 설정 위치

`settings.json`의 `"hooks"` 필드에 설정한다. 여러 scope에서 설정 가능하며, 상위 scope가 우선한다.

| scope | 파일 위치 | 공유 |
|-------|----------|------|
| User | `~/.claude/settings.json` | 개인 전역 |
| Project | `.claude/settings.json` | 팀 공유 (git 포함) |
| Project Local | `.claude/settings.local.json` | 개인 로컬 |

---

## 3. 설정 구조

```json
{
  "hooks": {
    "이벤트명": [
      {
        "matcher": "정규식 패턴",
        "hooks": [
          {
            "type": "command",
            "command": "실행할 명령",
            "timeout": 600
          }
        ]
      }
    ]
  }
}
```

- **이벤트명**: 언제 실행할지 (PreToolUse, PostToolUse 등)
- **matcher**: 어떤 도구/상황에서 실행할지 (정규식, 빈 문자열이면 전체 매칭)
- **hooks**: 실행할 명령 배열 (매칭된 훅은 병렬 실행)

---

## 4. 이벤트 종류

### 차단 가능한 이벤트

| 이벤트 | 시점 | matcher 대상 |
|--------|------|-------------|
| `UserPromptSubmit` | 사용자 프롬프트 제출 시 | 없음 |
| `PreToolUse` | 도구 실행 전 | 도구명 (`Bash`, `Edit`, `Write` 등) |
| `PermissionRequest` | 권한 대화상자 표시 시 | 도구명 |
| `Stop` | Claude 응답 완료 시 | 없음 |
| `TaskCompleted` | 태스크 완료 표시 시 | 없음 |
| `ConfigChange` | 설정 파일 변경 시 | 설정 소스명 |

### 차단 불가능한 이벤트

| 이벤트 | 시점 | matcher 대상 |
|--------|------|-------------|
| `SessionStart` | 세션 시작/재개 | `startup`, `resume`, `clear`, `compact` |
| `SessionEnd` | 세션 종료 | `clear`, `resume`, `logout` |
| `PostToolUse` | 도구 실행 성공 후 | 도구명 |
| `PostToolUseFailure` | 도구 실행 실패 후 | 도구명 |
| `Notification` | 알림 발생 | `permission_prompt`, `idle_prompt` |
| `SubagentStart` | 서브에이전트 생성 | 에이전트 타입 |
| `SubagentStop` | 서브에이전트 종료 | 에이전트 타입 |
| `PreCompact` | 컨텍스트 압축 전 | `manual`, `auto` |
| `PostCompact` | 컨텍스트 압축 후 | `manual`, `auto` |

---

## 5. Hook 타입

### command (가장 일반적)

셸 명령을 실행한다. stdin으로 JSON 입력을 받는다.

```json
{
  "type": "command",
  "command": "$CLAUDE_PROJECT_DIR/.claude/hooks/my-hook.sh",
  "timeout": 600
}
```

사용 가능한 환경변수:
- `CLAUDE_PROJECT_DIR` — 프로젝트 루트 경로
- `CLAUDE_ENV_FILE` — SessionStart 전용, 환경변수 영속화 파일 경로

### prompt (LLM 판단)

Claude 모델이 한 번의 턴으로 허용/차단을 판단한다.

```json
{
  "type": "prompt",
  "prompt": "이 명령이 안전한지 판단해라: $ARGUMENTS",
  "model": "haiku",
  "timeout": 30
}
```

### agent (복합 검증)

서브에이전트를 생성하여 파일 읽기, 명령 실행 등 복합 검증을 수행한다.

```json
{
  "type": "agent",
  "prompt": "테스트 스위트를 실행하고 결과를 검증해라. $ARGUMENTS",
  "timeout": 60
}
```

### http (웹훅)

HTTP POST로 JSON을 전송한다.

```json
{
  "type": "http",
  "url": "http://localhost:8080/hooks/validate",
  "headers": { "Authorization": "Bearer $MY_TOKEN" },
  "allowedEnvVars": ["MY_TOKEN"],
  "timeout": 30
}
```

---

## 6. 입출력

### 입력 (stdin JSON)

모든 hook은 stdin으로 JSON을 받는다:

```json
{
  "session_id": "abc123",
  "cwd": "/working/directory",
  "hook_event_name": "PreToolUse",
  "tool_name": "Bash",
  "tool_input": {
    "command": "npm test"
  }
}
```

### 출력 (exit code)

| exit code | 동작 |
|-----------|------|
| 0 | 정상 진행. stdout의 JSON 파싱 (있으면) |
| 2 | 차단. stderr 메시지를 Claude에게 전달 |
| 1, 3+ | 로깅만, 동작 진행 |

### JSON 출력 (exit 0일 때)

**PreToolUse에서 권한 제어:**
```json
{
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "allow",
    "additionalContext": "추가 컨텍스트"
  }
}
```

- `"allow"` — 권한 프롬프트 생략
- `"deny"` — 도구 호출 취소, reason을 Claude에게 전달
- `"ask"` — 일반 권한 프롬프트 표시

---

## 7. Matcher 패턴

matcher는 정규식이다. 대소문자를 구분한다.

| 패턴 | 매칭 대상 |
|------|----------|
| `Bash` | Bash 도구만 |
| `Edit\|Write` | Edit 또는 Write |
| `mcp__.*` | 모든 MCP 도구 |
| `mcp__github__.*` | GitHub MCP의 모든 도구 |
| `""` (빈 문자열) | 해당 이벤트의 모든 경우 |

MCP 도구 이름 형식: `mcp__{서버명}__{도구명}`

---

## 8. 실전 예시

### 위험한 명령 차단

`.claude/hooks/block-rm.sh`:
```bash
#!/bin/bash
INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command')

if echo "$COMMAND" | grep -q 'rm -rf'; then
  echo "Blocked: rm -rf 명령은 허용되지 않음" >&2
  exit 2
fi
exit 0
```

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [{ "type": "command", "command": "$CLAUDE_PROJECT_DIR/.claude/hooks/block-rm.sh" }]
      }
    ]
  }
}
```

### 코드 편집 후 자동 포맷팅

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [{ "type": "command", "command": "jq -r '.tool_input.file_path' | xargs npx prettier --write" }]
      }
    ]
  }
}
```

### 데스크톱 알림 (Linux)

```json
{
  "hooks": {
    "Notification": [
      {
        "matcher": "",
        "hooks": [{ "type": "command", "command": "notify-send 'Claude Code' 'Claude Code needs your attention'" }]
      }
    ]
  }
}
```

### 컨텍스트 압축 후 정보 재주입

```json
{
  "hooks": {
    "SessionStart": [
      {
        "matcher": "compact",
        "hooks": [{ "type": "command", "command": "echo 'Reminder: Bun 사용, npm 아님. 커밋 전 bun test 실행 필수.'" }]
      }
    ]
  }
}
```

---

## 9. 디버깅

| 방법 | 설명 |
|------|------|
| `/hooks` | 설정된 모든 hook 목록 확인 (읽기 전용) |
| `Ctrl+O` | Verbose 모드 토글, hook 출력을 트랜스크립트에서 확인 |
| `claude --debug` | 디버그 모드, hook 매칭/실행 상세 로그 |
| 수동 테스트 | `echo '{"tool_name":"Bash","tool_input":{"command":"npm test"}}' \| ./my-hook.sh && echo $?` |

### 주의사항

- 스크립트 실행 권한 필요: `chmod +x script.sh`
- 상대 경로 대신 `$CLAUDE_PROJECT_DIR` 사용 권장
- 셸 프로파일(~/.zshrc 등)의 echo 출력이 JSON 파싱을 방해할 수 있음 — `if [[ $- == *i* ]]`로 감싸기
- exit 2는 JSON 출력을 무시함 — exit code와 JSON을 동시에 사용하지 않기
- PostToolUse는 이미 실행 완료된 후이므로 되돌릴 수 없음
