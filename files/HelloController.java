package hello;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @RequestMapping("/")
        public String index() {
            return "GWF was here\n";
        }
    @RequestMapping("/hello")
        public String index2() {

            return "This is the hello world context!!!\n";
        }

}
