/*
 * Copyright 2018-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kfyty.reactive.r2dbc.query.templated;

import com.kfyty.reactive.r2dbc.query.templated.mapping.TemplateStatement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 动态模板 SQL 提供者基础实现
 *
 * @author kfyty725
 * @date 2023/10/21 18:31
 * @email kfyty725@hotmail.com
 */
@Slf4j
public abstract class AbstractDynamicTemplateProvider<TS extends TemplateStatement> implements DynamicTemplateProvider<TS>, ResourceLoaderAware, InitializingBean {
    /**
     * 空行正则
     */
    public static final Pattern BLANK_LINE_PATTERN = Pattern.compile("(?m)^\\s*$" + System.lineSeparator());

    /**
     * 要解析的模板路径
     */
    protected List<String> paths;

    /**
     * 资源扫描器
     */
    protected ResourcePatternResolver resourcePatternResolver;

    /**
     * 解析的动态模板 statement
     */
    protected Map<String, TS> templateStatements = new ConcurrentHashMap<>();

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
    }

    @Override
    public void setTemplatePath(List<String> paths) {
        this.paths = paths;
    }

    @Override
    public List<TS> resolve(List<String> paths) {
        try {
            List<TS> templateStatements = new ArrayList<>();
            for (String path : paths) {
                Resource[] resources = this.resourcePatternResolver.getResources(ResourceUtils.CLASSPATH_URL_PREFIX + path);
                for (Resource resource : resources) {
                    Element rootElement = createElement(resource.getInputStream());
                    String namespace = resolveAttribute(rootElement, TemplateStatement.TEMPLATE_NAMESPACE, () -> new IllegalArgumentException("namespace can't be empty"));
                    NodeList select = rootElement.getElementsByTagName(TemplateStatement.SELECT_LABEL);
                    NodeList execute = rootElement.getElementsByTagName(TemplateStatement.EXECUTE_LABEL);
                    templateStatements.addAll(this.resolveInternal(namespace, TemplateStatement.SELECT_LABEL, select));
                    templateStatements.addAll(this.resolveInternal(namespace, TemplateStatement.EXECUTE_LABEL, execute));
                    log.info("resolved resource: " + resource.getDescription());
                }
            }
            return templateStatements;
        } catch (IOException e) {
            throw new IllegalArgumentException("resolve template failed", e);
        }
    }

    @Override
    public String resolveTemplateStatementId(Method method) {
        String id = method.getDeclaringClass().getName() + "." + method.getName();
        if (!this.templateStatements.containsKey(id)) {
            throw new IllegalArgumentException("template statement doesn't exists of id: " + id);
        }
        return id;
    }

    @Override
    public void afterPropertiesSet() {
        List<TS> resolve = this.resolve(this.paths);
        resolve.forEach(e -> this.templateStatements.put(e.getId(), e));
    }

    /**
     * 内部解析，由子类实现
     *
     * @param namespace 命名空间
     * @param labelType sql type
     * @param nodeList  xml 元素列表
     * @return TemplateStatement
     */
    protected List<TS> resolveInternal(String namespace, String labelType, NodeList nodeList) {
        List<TS> templateStatements = new ArrayList<>(nodeList.getLength() + 1);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            String id = namespace + "." + resolveAttribute(element, TemplateStatement.TEMPLATE_STATEMENT_ID, () -> new IllegalArgumentException("id can't be empty"));
            templateStatements.add(this.resolveInternal(namespace, id, labelType, element.getTextContent()));
        }
        return templateStatements;
    }

    /**
     * 内部解析，由子类实现
     *
     * @param namespace 命名空间
     * @param id        statement id
     * @param labelType sql type
     * @param content   模板内容
     * @return TemplateStatement
     */
    protected abstract TS resolveInternal(String namespace, String id, String labelType, String content);

    /**
     * 从一个输入流创建一个 xml 元素
     */
    protected static Element createElement(InputStream inputStream) {
        try {
            return DocumentBuilderFactoryHolder.INSTANCE.newDocumentBuilder().parse(inputStream).getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("load dynamic template failed", e);
        }
    }

    /**
     * 解析 xml 元素属性值
     */
    protected static String resolveAttribute(Element element, String name, Supplier<RuntimeException> emptyException) {
        String attribute = element.getAttribute(name);
        if (emptyException != null && !StringUtils.hasText(attribute)) {
            throw emptyException.get();
        }
        return attribute;
    }

    private static final class DocumentBuilderFactoryHolder {
        static final DocumentBuilderFactory INSTANCE = DocumentBuilderFactory.newInstance();
    }
}
