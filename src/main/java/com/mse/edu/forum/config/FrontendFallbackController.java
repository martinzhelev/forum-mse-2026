package com.mse.edu.forum.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendFallbackController {

	@GetMapping(value = {
			"/",
			"/login",
			"/profile",
			"/maintenance"
	})
	public String frontend() {
		return "forward:/index.html";
	}
}
