package com.cycle.cache.http.web;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.LastModified;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@RestController
public class HttpCacheController implements LastModified {
	
	Cache<String, Long> lastModifiedCache = 
			CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();
	
	private long lastModifiedMillis; 
	
	@RequestMapping("/cache")
	public ResponseEntity<String> cache(
			@RequestHeader(value = "If-Modified-Since",required = false) Date ifModifiedSince) throws Exception  {
		DateFormat gmtDateFormat = 
				new SimpleDateFormat("EEE,d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
		
//		lastModifiedMillis = getLastModified() / 1000 * 1000 ;
		
		// 当前系统时间（去除毫秒值）
		long now = System.currentTimeMillis() / 1000 * 1000;
		// 文档可以在浏览器端/proxy上缓存多久（单位:秒）
		long maxAge = 20;
		
		// 判断内容是否修改了，此处使用等值判断
		if (ifModifiedSince != null
				&& ifModifiedSince.getTime() == lastModifiedMillis) {
			MultiValueMap<String, String> headers = new HttpHeaders();
			// 当前时间
			headers.add("Date", gmtDateFormat.format(new Date(now)));
			// 过期时间 http 1.0 支持
			headers.add("Expires", gmtDateFormat.format(new Date(now + maxAge * 1000 )));
			// 文档生存时间 http 1.1 支持
			headers.add("Cache-Control", "max-age=" + maxAge);
			return new ResponseEntity<>(headers,HttpStatus.NOT_MODIFIED);
		}
		
		String body = "<a href=''>点击访问链接abv</>";
		MultiValueMap<String, String> headers = new HttpHeaders();
		// 当前时间
		headers.add("Date", gmtDateFormat.format(new Date(now)));
		// 文档修改时间
		headers.add("Last-Modified", gmtDateFormat.format(new Date(lastModifiedMillis)));
		// 过期时间 http 1.0 支持 
		headers.add("Expires", gmtDateFormat.format(new Date(now + maxAge * 1000)));
		// 文档生存时间 http 1.1 支持
		headers.add("Cache-Control", "max-age=" + maxAge);
		System.out.println(headers);
		return new ResponseEntity<>(body,headers,HttpStatus.OK);
	}
	
	private long getLastModified() throws ExecutionException {
		return lastModifiedCache.get("lastModified", ()-> {return System.currentTimeMillis();} );
	}

	@Override
	public long getLastModified(HttpServletRequest arg0) {
		if (lastModifiedMillis == 0) {
			try {
				// 文档最后修改时间（去掉毫秒值）（为方便测试，每10秒生成一个新的）
				lastModifiedMillis = getLastModified() / 1000 * 1000 ;
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		return 0;
	}
	
}
