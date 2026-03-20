# Claude Code 플러그인

플러그인은 스킬, 에이전트, 훅, MCP 서버, LSP 서버를 하나의 패키지로 묶어 배포하는 확장 시스템이다.
개별 설정을 하나씩 구성하는 대신, 플러그인 하나로 관련 기능을 일괄 설치한다.

---

## 1. 플러그인 vs 개별 설정

| 상황 | 권장 방식 |
|------|----------|
| 단일 스킬이나 에이전트만 필요 | `.claude/skills/` 또는 `.claude/agents/`에 직접 생성 |
| 여러 구성요소를 묶어 배포 | 플러그인 |
| 팀/조직 전체에 표준화 | 플러그인 + 마켓플레이스 |

---

## 2. 플러그인 구조

```
my-plugin/
├── manifest.json          # 플러그인 메타데이터 (필수)
├── skills/
│   └── deploy/
│       └── SKILL.md       # 스킬 정의
├── agents/
│   └── reviewer.md        # 에이전트 정의
├── hooks/
│   └── pre-commit.sh      # 훅 스크립트
├── mcp-servers/
│   └── config.json        # MCP 서버 설정
└── lsp-servers/
    └── config.json        # LSP 서버 설정
```

---

## 3. manifest.json

```json
{
  "name": "my-plugin",
  "version": "1.0.0",
  "description": "배포 자동화 플러그인",
  "author": "team-name",
  "skills": "skills/",
  "agents": "agents/",
  "hooks": "hooks.json",
  "mcpServers": "mcp-servers/",
  "lspServers": "lsp-servers/"
}
```

### 필수 필드

| 필드 | 설명 |
|------|------|
| `name` | 플러그인 식별자 |
| `version` | 시맨틱 버전 |

### 선택 필드

| 필드 | 설명 |
|------|------|
| `description` | 플러그인 설명 |
| `author` | 제작자 |
| `skills` | 스킬 디렉토리 경로 |
| `agents` | 에이전트 디렉토리 경로 |
| `hooks` | 훅 설정 JSON 경로 |
| `mcpServers` | MCP 서버 설정 경로 |
| `lspServers` | LSP 서버 설정 경로 |

---

## 4. 구성요소

### 스킬

`skills/{skill-name}/SKILL.md` 형태로 배치한다. 일반 스킬과 동일한 frontmatter를 지원한다. 플러그인 스킬은 `plugin-name:skill-name`으로 네임스페이스가 지정된다.

### 에이전트

`agents/{agent-name}.md` 형태로 배치한다. 일반 서브에이전트와 동일한 frontmatter를 지원한다.

### 훅

21개 이벤트 타입을 지원한다. 일반 훅과 동일한 형식이다.

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [{
          "type": "command",
          "command": "${CLAUDE_PLUGIN_ROOT}/hooks/format.sh"
        }]
      }
    ]
  }
}
```

### MCP 서버

플러그인 내 MCP 서버를 정의한다.

### LSP 서버

코드 인텔리전스(자동완성, 진단, 정의 이동 등)를 위한 LSP 서버를 번들한다.

---

## 5. 환경변수

| 변수 | 설명 |
|------|------|
| `${CLAUDE_PLUGIN_ROOT}` | 플러그인 루트 디렉토리 경로 |
| `${CLAUDE_PLUGIN_DATA}` | 플러그인 영속 데이터 디렉토리 경로 |

---

## 6. 로컬 테스트

```bash
# 로컬 디렉토리에서 플러그인 테스트
claude --plugin-dir ./my-plugin
```

---

## 7. CLI 명령

```bash
claude plugin install <name>          # 플러그인 설치
claude plugin uninstall <name>        # 플러그인 제거
claude plugin enable <name>           # 플러그인 활성화
claude plugin disable <name>          # 플러그인 비활성화
claude plugin update                  # 플러그인 업데이트
```

---

## 8. 마켓플레이스

마켓플레이스는 플러그인을 검색하고 설치할 수 있는 배포 채널이다.

### 공식 마켓플레이스

Anthropic이 운영하는 공식 마켓플레이스에서 검증된 플러그인을 설치할 수 있다.

### 주요 플러그인 카테고리

| 카테고리 | 예시 |
|----------|------|
| 코드 인텔리전스 | TypeScript, Python, Go, Rust 등 LSP 플러그인 |
| 외부 통합 | GitHub, GitLab, Jira, Slack 연동 |
| 개발 워크플로우 | 코드 리뷰, 프론트엔드 디자인, 스킬 생성 |
| 출력 스타일 | 응답 형식 커스터마이징 |

### 마켓플레이스 추가

settings.json의 `extraKnownMarketplaces`로 등록한다:

```json
{
  "extraKnownMarketplaces": {
    "acme-tools": {
      "source": "github",
      "repo": "acme-corp/plugins"
    },
    "internal": {
      "source": "git",
      "url": "https://gitlab.example.com/plugins.git"
    }
  }
}
```

### 설치

```bash
claude plugin install formatter
```

---

## 9. marketplace.json

커스텀 마켓플레이스를 만들려면 `marketplace.json`을 작성한다.

```json
{
  "name": "acme-plugins",
  "owner": "Acme Corp",
  "description": "사내 Claude Code 플러그인",
  "plugins": [
    {
      "name": "formatter",
      "version": "1.2.0",
      "description": "코드 포맷팅 자동화",
      "source": {
        "type": "github",
        "repo": "acme-corp/formatter-plugin"
      }
    }
  ]
}
```

### 플러그인 소스 타입

| 타입 | 설명 | 예시 |
|------|------|------|
| `github` | GitHub 레포지토리 | `"repo": "acme-corp/plugin"` |
| `git` | Git URL | `"url": "https://gitlab.example.com/plugin.git"` |
| `git-subdir` | Git 레포 내 서브디렉토리 | `"repo": "...", "path": "plugins/my-plugin"` |
| `npm` | NPM 패키지 | `"package": "@acme/plugin"` |
| `pip` | Python 패키지 | `"package": "acme-plugin"` |

---

## 10. 설정

### settings.json

```json
{
  "enabledPlugins": {
    "formatter@acme-tools": true,
    "deployer@acme-tools": false
  },
  "extraKnownMarketplaces": {
    "acme-tools": {
      "source": "github",
      "repo": "acme-corp/claude-plugins"
    }
  }
}
```

### Managed 설정 (조직 정책)

```json
{
  "strictKnownMarketplaces": [
    { "source": "github", "repo": "acme-corp/approved-plugins" }
  ],
  "blockedMarketplaces": [
    { "source": "github", "repo": "untrusted/plugins" }
  ]
}
```

| 설정 | 동작 |
|------|------|
| `strictKnownMarketplaces` 미설정 | 제한 없음 |
| 빈 배열 `[]` | 모든 마켓플레이스 차단 |
| 목록 지정 | 지정된 소스만 허용 |

---

## 11. 트러블슈팅

| 문제 | 해결 |
|------|------|
| 플러그인이 로드되지 않음 | `manifest.json` 존재 확인, `claude plugin list`로 상태 확인 |
| 스킬이 보이지 않음 | 네임스페이스 `plugin-name:skill-name` 확인 |
| 훅이 실행되지 않음 | 스크립트 실행 권한 확인 (`chmod +x`) |
| 업데이트가 반영되지 않음 | `claude plugin update` 실행, 캐시 확인 |
