package com.inhouse.inhouse_manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 메모리 DB를 쓴다. 파일 DB는 실행 중인 서버가 잠그고 있어서 같이 열 수 없다
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:contextLoads")
class InhouseManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
