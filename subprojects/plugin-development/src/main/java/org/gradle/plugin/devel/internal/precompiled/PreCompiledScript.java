/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.plugin.devel.internal.precompiled;

import com.google.common.base.CaseFormat;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.invocation.Gradle;
import org.gradle.configuration.ScriptTarget;
import org.gradle.groovy.scripts.ScriptSource;
import org.gradle.groovy.scripts.TextResourceScriptSource;
import org.gradle.internal.hash.HashCode;
import org.gradle.internal.resource.UriTextResource;
import org.gradle.plugin.use.PluginId;
import org.gradle.plugin.use.internal.DefaultPluginId;

import java.io.File;

class PreCompiledScript {
    public static final String SCRIPT_PLUGIN_EXTENSION = ".gradle";

    private final ScriptSource scriptSource;
    private final Type type;
    private final PluginId pluginId;

    private enum Type {
        PROJECT(Project.class, ProjectPluginAdapter.class, SCRIPT_PLUGIN_EXTENSION),
        SETTINGS(Settings.class, SettingsPluginAdapter.class, ".settings.gradle"),
        INIT(Gradle.class, InitPluginAdapter.class, ".init.gradle");

        private final Class<?> targetClass;
        private final Class<?> adapterClass;
        private final String fileExtension;

        Type(Class<?> targetClass, Class<?> adapterClass, String fileExtension) {
            this.targetClass = targetClass;
            this.adapterClass = adapterClass;
            this.fileExtension = fileExtension;
        }

        static Type getType(String fileName) {
            if (fileName.endsWith(SETTINGS.fileExtension)) {
                return SETTINGS;
            }
            if (fileName.endsWith(INIT.fileExtension)) {
                return INIT;
            }
            return PROJECT;
        }

        PluginId toPluginId(String fileName) {
            return DefaultPluginId.of(fileName.substring(0, fileName.indexOf(fileExtension)));
        }
    }

    PreCompiledScript(File scriptFile) {
        this.scriptSource = new TextResourceScriptSource(new UriTextResource("script", scriptFile));
        String fileName = scriptFile.getName();
        this.type = Type.getType(fileName);
        this.pluginId = type.toPluginId(fileName);
    }

    String getId() {
        return pluginId.getId();
    }

    String getGeneratedPluginClassName() {
        return toJavaIdentifier(kebabCaseToPascalCase(pluginId.getName())) + "Plugin";
    }

    File getScriptFile() {
        return new File(scriptSource.getFileName());
    }

    String getClassName() {
        return scriptSource.getClassName();
    }

    String getPluginsBlockClassName() {
        return getPluginsBlockSource().getClassName();
    }

    HashCode getContentHash() {
        return scriptSource.getResource().getContentHash();
    }

    ScriptSource getSource() {
        return scriptSource;
    }

    ScriptSource getPluginsBlockSource() {
        return new PluginsBlockSourceWrapper(scriptSource);
    }

    private static String kebabCaseToPascalCase(String s) {
        return CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, s);
    }

    private static String toJavaIdentifier(String s) {
        StringBuilder sb = new StringBuilder();
        if (!Character.isJavaIdentifierStart(s.charAt(0))) {
            sb.append("_");
        }
        for (char c : s.toCharArray()) {
            if (Character.isJavaIdentifierPart(c)) {
                sb.append(c);
            } else {
                sb.append("_");
            }
        }
        return sb.toString();
    }

    public ScriptTarget getScriptTarget() {
        return new PreCompiledScriptTarget();
    }

    public Class<?> getTargetClass() {
        return type.targetClass;
    }

    public Class<?> getAdapterClass() {
        return type.adapterClass;
    }

}
