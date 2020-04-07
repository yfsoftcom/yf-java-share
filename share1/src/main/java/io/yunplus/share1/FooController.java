package io.yunplus.share1;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class FooController {

	@RequestMapping("/")
	public String index() {
		return "Greetings from Spring Boot!";
	}

}