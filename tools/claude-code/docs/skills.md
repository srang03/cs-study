# Claude Code 스킬

스킬은 Claude의 능력을 확장하는 재사용 가능한 지침이다.
`SKILL.md` 파일에 지침을 작성하면 Claude가 도구로 추가한다.
관련 상황에서 자동 로드되거나, `/skill-name`으로 직접 호출할 수 있다.

> `.claude/commands/`의 커스텀 명령은 스킬로 통합되었다. 기존 commands 파일은 그대로 동작하며, 스킬은 지원 파일, frontmatter 등 추가 기능을 제공한다.

> 스킬은 [Agent Skills](https://agentskills.io) 오픈 스탠다드를 따른다.

---

## 1. 번들 스킬

Claude Code에 기본 탑재된 스킬이다. 모든 세션에서 사용 가능하다.

| 스킬 | 용도 |
|------|------|
| `/batch <instruction>` | 코드베이스 전체에 대규모 변경을 병렬 실행. 5~30개 단위로 분해, 각각 git worktree에서 독립 수행 후 PR 생성 |
| `/claude-api` | Claude API/SDK 레퍼런스 로드. `anthropic` import 시 자동 활성화 |
| `/debug [description]` | 현재 세션 디버그 로그 분석 |
| `/loop [interval] <prompt>` | 프롬프트를 주기적으로 반복 실행 (기본 10분) |
| `/simplify [focus]` | 변경된 코드의 재사용성, 품질, 효율성 검토 후 수정. 3개 리뷰 에이전트를 병렬 실행 |

---

## 2. 스킬 생성

### 디렉토리 생성

```bash
# 개인 스킬 (모든 프로젝트에서 사용)
mkdir -p ~/.claude/skills/explain-code

# 프로젝트 스킬 (현재 프로젝트 전용)
mkdir -p .claude/skills/explain-code
```

### SKILL.md 작성

```yaml
---
name: explain-code
description: 코드를 다이어그램과 비유로 설명한다. "이 코드가 어떻게 동작해?" 같은 질문에 사용
---

코드를 설명할 때 다음을 포함한다:

1. **비유**: 일상 사물에 비유
2. **다이어그램**: ASCII 아트로 흐름/구조 표현
3. **단계별 설명**: 실행 순서 설명
4. **주의점**: 흔한 실수나 오해 지적
```

### 테스트

```
# 자동 호출
이 코드가 어떻게 동작해?

# 직접 호출
/explain-code src/auth/login.ts
```

---

## 3. 스킬 저장 위치

| 위치 | 경로 | 적용 범위 |
|------|------|----------|
| Enterprise | Managed settings | 조직 전체 |
| Personal | `~/.claude/skills/{name}/SKILL.md` | 모든 프로젝트 |
| Project | `.claude/skills/{name}/SKILL.md` | 현재 프로젝트 |
| Plugin | `{plugin}/skills/{name}/SKILL.md` | 플러그인 활성화 시 |

우선순위: enterprise > personal > project. 플러그인 스킬은 `plugin-name:skill-name` 네임스페이스를 사용하므로 충돌하지 않는다.

### 자동 탐색

서브디렉토리의 `.claude/skills/`도 자동 탐색된다. 모노레포에서 패키지별 스킬을 지원한다.

### 디렉토리 구조

```
my-skill/
├── SKILL.md           # 메인 지침 (필수)
├── template.md        # Claude가 채울 템플릿
├── examples/
│   └── sample.md      # 기대 출력 예시
└── scripts/
    └── validate.sh    # Claude가 실행할 스크립트
```

---

## 4. Frontmatter 필드

| 필드 | 필수 | 설명 |
|------|------|------|
| `name` | 아니오 | 스킬 이름 (생략 시 디렉토리명 사용). 소문자, 숫자, 하이픈만 (최대 64자) |
| `description` | 권장 | 스킬 용도. Claude가 자동 로드 판단에 사용 |
| `argument-hint` | 아니오 | 자동완성 시 인자 힌트 (예: `[issue-number]`) |
| `disable-model-invocation` | 아니오 | `true`면 Claude 자동 호출 방지. 수동 호출만 가능 |
| `user-invocable` | 아니오 | `false`면 `/` 메뉴에서 숨김. Claude만 사용하는 배경 지식용 |
| `allowed-tools` | 아니오 | 스킬 활성 시 승인 없이 사용 가능한 도구 |
| `model` | 아니오 | 스킬 실행 시 사용할 모델 |
| `context` | 아니오 | `fork` 설정 시 서브에이전트에서 격리 실행 |
| `agent` | 아니오 | `context: fork` 시 사용할 서브에이전트 타입 |
| `hooks` | 아니오 | 스킬 생명주기에 스코핑된 훅 |

### 호출 제어

| frontmatter | 사용자 호출 | Claude 호출 | 컨텍스트 로딩 |
|-------------|-----------|------------|-------------|
| (기본값) | 가능 | 가능 | description 항상 로드, 호출 시 전체 로드 |
| `disable-model-invocation: true` | 가능 | 불가 | 컨텍스트에 미포함 |
| `user-invocable: false` | 불가 | 가능 | description 항상 로드, 호출 시 전체 로드 |

---

## 5. 문자열 치환

| 변수 | 설명 |
|------|------|
| `$ARGUMENTS` | 호출 시 전달된 모든 인자 |
| `$ARGUMENTS[N]` 또는 `$N` | N번째 인자 (0-based) |
| `${CLAUDE_SESSION_ID}` | 현재 세션 ID |
| `${CLAUDE_SKILL_DIR}` | SKILL.md가 위치한 디렉토리 |

```yaml
---
name: fix-issue
description: GitHub 이슈를 수정한다
disable-model-invocation: true
---

GitHub 이슈 $ARGUMENTS를 수정한다.

1. 이슈 설명 읽기
2. 요구사항 파악
3. 수정 구현
4. 테스트 작성
5. 커밋 생성
```

`/fix-issue 123` 실행 시 `$ARGUMENTS`가 `123`으로 치환된다.

---

## 6. 동적 컨텍스트 주입

`` !`command` `` 문법으로 셸 명령의 출력을 스킬 내용에 삽입한다. Claude가 보기 전에 전처리로 실행된다.

```yaml
---
name: pr-summary
description: PR 변경 사항을 요약한다
context: fork
agent: Explore
allowed-tools: Bash(gh *)
---

## PR 컨텍스트
- 변경 diff: !`gh pr diff`
- 코멘트: !`gh pr view --comments`
- 변경 파일: !`gh pr diff --name-only`

## 작업
이 PR을 요약해라...
```

---

## 7. 서브에이전트에서 실행

`context: fork`를 설정하면 격리된 서브에이전트에서 실행된다. 대화 이력에 접근할 수 없다.

```yaml
---
name: deep-research
description: 주제를 심층 조사한다
context: fork
agent: Explore
---

$ARGUMENTS를 심층 조사한다:

1. Glob과 Grep으로 관련 파일 탐색
2. 코드 분석
3. 파일 참조와 함께 결과 요약
```

| 접근 | 시스템 프롬프트 | 작업 | 로드 대상 |
|------|---------------|------|----------|
| 스킬 + `context: fork` | 에이전트 타입에서 상속 | SKILL.md 내용 | CLAUDE.md |
| 서브에이전트 + `skills` 필드 | 에이전트 마크다운 본문 | Claude 위임 메시지 | 사전 로드 스킬 + CLAUDE.md |

`agent` 필드 옵션: `Explore`, `Plan`, `general-purpose`, 또는 `.claude/agents/`의 커스텀 에이전트.

---

## 8. 스킬 접근 제한

### 전체 스킬 비활성화

```
# /permissions에서 deny 추가
Skill
```

### 특정 스킬만 허용/거부

```
# 특정 스킬 허용
Skill(commit)
Skill(review-pr *)

# 특정 스킬 거부
Skill(deploy *)
```

---

## 9. 트러블슈팅

### 스킬이 트리거되지 않을 때

1. description에 사용자가 자연스럽게 말할 키워드를 포함하는지 확인
2. `What skills are available?`로 스킬 목록 확인
3. `/skill-name`으로 직접 호출 시도

### 스킬이 너무 자주 트리거될 때

1. description을 더 구체적으로 작성
2. `disable-model-invocation: true` 추가

### Claude가 모든 스킬을 인식하지 못할 때

스킬 description은 컨텍스트 윈도우의 2% (폴백 16,000자) 예산 내에서 로드된다. `/context`로 제외된 스킬을 확인할 수 있다. `SLASH_COMMAND_TOOL_CHAR_BUDGET` 환경변수로 한도를 조정한다.
