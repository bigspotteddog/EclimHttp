package com.nobodyelses.httpserver;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Test;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class MustacheTest extends TestCase {

    @Test
    public void test() {

        Map<String, String> map = new HashMap<String, String>();
        map.put("hello", "bob");

        String template = "<div>{{hello}}</div>";
        StringReader reader = new StringReader(template);

        String result = template(reader, map);

        assertEquals("", result);
    }

    private String template(StringReader reader, Map<String, String> map) {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache compile = mf.compile(reader, null);
        StringWriter writer = new StringWriter();
        compile.execute(writer, map);
        String result = writer.toString();
        return result;
    }

    public void testHandlebars() throws Exception {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> item = new HashMap<String, Object>();
        item.put("message", "message1");
        item.put("filename", "filename1");
        item.put("line", 12);
        item.put("column", 5);
        list.add(item);

        Handlebars handlebars = new Handlebars();
        Template compile = handlebars.compile("templates/problems.html");
        String html = compile.apply(list);
        assertEquals("", html);
    }
}
