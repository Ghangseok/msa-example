-- DB가 리스너에 서비스(XEPDB1 등)를 등록할 때 쓰는 주소를 IPv4 루프백으로 고정한다.
--
-- 왜: compose 컨테이너가 붙는 kind 네트워크는 IPv6를 켜 둔다. 그래서 컨테이너 호스트 이름이 IPv6 주소로도 풀린다.
-- LOCAL_LISTENER가 비어 있으면 DB는 호스트 이름으로 리스너를 찾는다. 그런데 리스너는 IPv4(0.0.0.0:1521)에서만 듣는다.
-- 그러면 서비스가 등록되지 않고, 접속할 때 ORA-12514가 난다. (2026-10-05 확인: IPv6 주소로는 등록되지 않고 IPv4 주소로는 등록된다)
--
-- gvenzl/oracle-xe 이미지는 DB를 처음 만들 때만 /container-entrypoint-initdb.d의 SQL을 실행한다.
-- SCOPE=BOTH라서 설정이 데이터 볼륨의 spfile에 남고, 컨테이너를 다시 만들어도 유지된다.
ALTER SYSTEM SET LOCAL_LISTENER='(ADDRESS=(PROTOCOL=TCP)(HOST=127.0.0.1)(PORT=1521))' SCOPE=BOTH;
ALTER SYSTEM REGISTER;
