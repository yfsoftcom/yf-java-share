package io.yunplus.share1;

import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class FooController {

	@Autowired
	FooService fooService;

	@RequestMapping("/")
	public String index() {
		Map<String, Object> datas = new HashMap<>();
		datas.put("n1", "Jacob");
		datas.put("n2", "Stack");
		datas.put("v1", BigDecimal.valueOf(19.2));
		datas.put("v2", BigDecimal.valueOf(21.2));
		List<Map<String, Object>> lists = new ArrayList<>();
		lists.add(datas);

		File tempFile = null;
		try{
			tempFile = File.createTempFile("Report_Foo_", ".xlsx");
			FileOutputStream fileOut = new FileOutputStream(tempFile);
	
			FileInputStream fis = fooService.getTheReportTemplate("classpath:test.xlsx",
						"806dc9a969a8936c54768d1239e24b24",
						"");
	
			URI uri = FooController.class.getClassLoader()
				.getResource("rule.json")
				.toURI();
			Path path = Paths.get(uri);
			String rules = Files.lines(path, StandardCharsets.UTF_8).filter(x -> x != null && !x.contains("#")&&!x.startsWith("//"))
			.reduce((x, y) -> x + y).orElse("{}");
	
			boolean isOk = fooService.fillTheReport(rules, lists, fis, fileOut);
			return "result: " + isOk + "! file is " + tempFile.getAbsolutePath();
		}catch(Exception ex){
			ex.printStackTrace();
		}finally {
            if(tempFile != null){
                // remove the temp file
                // if(tempFile.exists()){
                //     tempFile.delete();
                // }
            }

		}
		return "Ops";
		
		
	}

}