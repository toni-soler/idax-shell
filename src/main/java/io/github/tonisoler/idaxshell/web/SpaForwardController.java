package io.github.tonisoler.idaxshell.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
  @GetMapping({"/ledger", "/ledger/**", "/ostris", "/ostris/**"})
  public String moduleRoute() { return "forward:/index.html"; }
}
