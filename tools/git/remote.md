git remote
remote repo는 인터넷에 프로젝트를 저장하는 공간
local repo -> remote repo 업로드하여 프로젝트 협업 수행
여러 사람의 하나의 remote repo에 연결되어 있다.
각자 local에서 작업한 내용을 remote에 push 하기도 하고, remote에 있는 작업한 내용을 pull해서 local에 가져와서 작업하기도 한다.
여기서 중요한건 local과 remote의 작업 내용(파일 상태)이 항상 동기화 되지 않는다.


git remote add <별명> <remote url>
git remote add origin https://github.com..
remote url의 repo를 별명 이름으로 연결하겠다
(remote url이 너무 길어서 짧은 별명으로 부르고 주로 origin 이라고 한다.)

git remote -v 하면 연결된 remote repo 정보를 확인할 수 있다.

