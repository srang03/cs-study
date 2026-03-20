# Claude Code 서브에이전트

서브에이전트는 특정 작업 유형을 처리하는 전문화된 AI 어시스턴트다.
단일 세션 내에서 실행되며, 각자 독립된 컨텍스트 윈도우를 가진다.

---

## 1. 내장 서브에이전트

| 에이전트 | 모델 | 도구 | 용도 |
|----------|------|------|------|
| Explore | Haiku | 읽기 전용 (Read, Glob, Grep 등) | 코드베이스 탐색, 빠른 검색 |
| Plan | 상위 모델 상속 | 읽기 전용 | 설계 계획 수립 |
| General-purpose | 상위 모델 상속 | 전체 도구 | 범용 복잡한 작업 |
| Bash | — | Bash 전용 | 셸 명령 실행 |
| statusline-setup | — | Read, Edit | 상태줄 설정 |
| Claude Code Guide | — | Glob, Grep, Read, WebFetch, WebSearch | Claude Code 사용법 안내 |

---

## 2. 커스텀 서브에이전트 생성

### 빠른 시작

`/agents` 명령으로 대화형 생성이 가능하다.

### 파일 생성

`.claude/agents/` 또는 `~/.claude/agents/`에 마크다운 파일을 작성한다.

```markdown
---
name: code-reviewer
description: 코드 품질과 보안을 검토하는 에이전트
model: sonnet
tools:
  - Read
  - Grep
  - Glob
---

코드 리뷰를 수행한다:

1. 보안 취약점 확인
2. 성능 문제 식별
3. 코딩 표준 준수 여부 점검
4. 개선 제안 제공
```

---

## 3. Scope와 우선순위

| scope | 위치 | 적용 범위 |
|-------|------|----------|
| CLI 플래그 | `--agent` | 해당 세션 (최우선) |
| Project | `.claude/agents/` | 현재 프로젝트 |
| User | `~/.claude/agents/` | 모든 프로젝트 |
| Plugin | 플러그인 내 `agents/` | 플러그인 활성화 시 |

동일 이름이 여러 scope에 존재하면 상위 scope가 우선한다.

---

## 4. Frontmatter 필드

| 필드 | 필수 | 설명 |
|------|------|------|
| `name` | 아니오 | 에이전트 이름. 생략 시 파일명 사용 |
| `description` | 권장 | 용도 설명. Claude가 자동 위임 판단에 사용 |
| `tools` | 아니오 | 사용 가능 도구 허용 목록 |
| `disallowedTools` | 아니오 | 사용 금지 도구 목록 |
| `model` | 아니오 | 사용 모델 (`sonnet`, `opus`, `haiku`) |
| `permissionMode` | 아니오 | 권한 모드 (`default`, `acceptEdits`, `dontAsk`, `bypassPermissions`, `plan`) |
| `maxTurns` | 아니오 | 최대 턴 수 제한 |
| `skills` | 아니오 | 사전 로드할 스킬 목록 |
| `mcpServers` | 아니오 | MCP 서버 설정 (인라인 정의 또는 이름 참조) |
| `hooks` | 아니오 | 에이전트 전용 훅 (`PreToolUse`, `PostToolUse`, `Stop`) |
| `memory` | 아니오 | 영속 메모리 scope (`user`, `project`, `local`) |
| `background` | 아니오 | 백그라운드 실행 여부 |
| `effort` | 아니오 | 추론 깊이 설정 |
| `isolation` | 아니오 | `worktree` 설정 시 격리된 git worktree에서 실행 |

---

## 5. 도구 제어

### 허용 목록

```yaml
tools:
  - Read
  - Grep
  - Glob
  - Bash(npm test *)
```

### 금지 목록

```yaml
disallowedTools:
  - Edit
  - Write
  - Agent(Explore)
```

`Agent(agent_type)` 문법으로 서브에이전트 스폰도 제한할 수 있다.

---

## 6. MCP 서버 스코핑

### 이름 참조 (기존 등록된 서버)

```yaml
mcpServers:
  - github
  - notion
```

### 인라인 정의

```yaml
mcpServers:
  custom-api:
    type: http
    url: https://api.example.com/mcp
    headers:
      Authorization: Bearer ${API_TOKEN}
```

---

## 7. 영속 메모리

서브에이전트가 세션 간에 학습 내용을 유지할 수 있다.

```yaml
memory: project
```

| scope | 저장 위치 | 공유 |
|-------|----------|------|
| `user` | `~/.claude/agent-memory/{agent-name}/` | 모든 프로젝트에서 공유 |
| `project` | `.claude/agent-memory/{agent-name}/` | 버전 관리로 팀 공유 가능 |
| `local` | `.claude/agent-memory-local/{agent-name}/` | 개인 전용 (git 미포함) |

메모리가 활성화되면 에이전트 시스템 프롬프트에 메모리 읽기/쓰기 지침이 자동 포함되고, `MEMORY.md`의 첫 200줄이 로드된다.

---

## 8. 실행 패턴

### 자동 위임

`description`이 설정되어 있으면 Claude가 작업에 적합한 에이전트를 자동으로 선택한다.

### @-mention

```
@code-reviewer 이 PR을 검토해줘
```

### 세션 전체 적용

```bash
claude --agent code-reviewer
```

### 포그라운드 vs 백그라운드

- **포그라운드** (기본값): 결과를 기다린 후 다음 작업 진행
- **백그라운드**: 완료 시 알림. 독립적인 작업에 적합

### 서브에이전트 재개

완료되지 않은 서브에이전트에 `SendMessage`로 추가 지시를 보낼 수 있다.

### 자동 컴팩션

컨텍스트 윈도우가 가득 차면 서브에이전트가 자동으로 컴팩션을 수행한다.

---

## 9. 훅

### 에이전트 내부 훅 (frontmatter)

```yaml
hooks:
  PreToolUse:
    - matcher: Bash
      hooks:
        - type: command
          command: echo "Bash command intercepted"
```

### 프로젝트 레벨 훅 (settings.json)

```json
{
  "hooks": {
    "SubagentStart": [
      {
        "matcher": "code-reviewer",
        "hooks": [{ "type": "command", "command": "echo 'Reviewer started'" }]
      }
    ],
    "SubagentStop": [
      {
        "matcher": "",
        "hooks": [{ "type": "command", "command": "echo 'Agent stopped'" }]
      }
    ]
  }
}
```

---

## 10. 예시: 디버거 에이전트

`.claude/agents/debugger.md`:

```markdown
---
name: debugger
description: 버그를 체계적으로 조사하고 수정하는 에이전트
model: opus
tools:
  - Read
  - Grep
  - Glob
  - Bash
  - Edit
  - Write
---

버그를 디버깅한다:

1. 재현 단계를 확인한다
2. 관련 코드를 탐색하고 분석한다
3. 로그와 에러 메시지를 조사한다
4. 근본 원인을 파악한다
5. 수정 사항을 구현하고 테스트한다
```

---

## 11. 서브에이전트 vs 에이전트 팀

| 항목 | 서브에이전트 | 에이전트 팀 |
|------|-------------|-----------|
| 통신 | 리더에게만 보고 | 팀원끼리 직접 통신 |
| 컨텍스트 | 독립 (리더 대화 미상속) | 독립 (리더 대화 미상속) |
| 조율 | 리더가 모든 작업 조율 | Task list 기반 자기 조율 |
| 토큰 비용 | 낮음 (결과만 요약) | ~7배 이상 (팀 규모에 비례) |
| 적합한 작업 | 빠른 단일 집중 작업 | 병렬 연구, 교차 계층 작업 |
