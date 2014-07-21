package com.nobodyelses.httpserver;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Test;

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
}
