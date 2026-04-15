# Database Migration Guide: H2 to MariaDB

이 프로젝트의 데이터베이스 엔진을 기존 내장형 H2에서 외부 Docker 컨테이너로 실행 중인 MariaDB로 변경합니다. 아래 명시된 접속 정보를 기반으로 설정 파일 및 의존성을 업데이트하십시오.

## 1. MariaDB 접속 정보 (Connection Info)
- **Host**: `192.168.10.30`
- **Port**: `3307`
- **Database**: `gitdigest`
- **Username**: `hubilon`
- **Password**: `hubilon123!@#`
- **Driver**: `org.mariadb.jdbc.Driver`
- **Dialect**: `org.hibernate.dialect.MariaDBDialect`
