/*-
 * #%L
 * Jackson Bean Tree
 * %%
 * Copyright (C) 2022 Hunter Strategy LLC
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
/**
 * # Jackson Bean Tree
 *
 * This module provides facilities to map file trees of serialized configuration
 * data to a single top-level bean. It uses
 * [Jackson](https://github.com/FasterXML/jackson) as its mapper utility, so that
 * clients can rely on a full and customizable feature set for the bulk of the
 * work. Additionally, any structured text format supported by Jackson can be used
 * with Jackson Bean Tree, such as YAML or TOML. This module uses Java NIO Paths so
 * that Java virtualized file systems can be the source of configuration - zip
 * files, normal file systems, or any Java NIO FileSystem implementation that you
 * may want to use, such as `git` trees at specific points in the graph.
 *
 * This concept is useful to reduce nesting complexity of configuration, to
 * integrate external files into a single serialization pass, or to use multi-file
 * configuration strategies like `.d` directories, or to use a repeating structure
 * to configure multiple components. These sorts of schemes can fit well into
 * "ConfigOps" pipelines - automation techniques similar to GitOps, where
 * configuration changes may be used to drive pipelines in the target system. A
 * pipeline can load a configuration tree, compare it as an object graph to a
 * previous version of the same configuration tree, and then make intelligent
 * decisions about any further actions to take.
 *
 * Using `git` as a source also allows configuration changes to be driven by
 * gitflow, review tools, or whatever other quality control and/or review process
 * makes sense for your organization or system.
 *
 * # Usage
 *
 * 1. Choose or create a top-level bean.
 * 2. Annotate this bean and its child objects with necessary Jackson annotations.
 * 3. Use @Bean and @BeanCollection annotations to map sibling files or
 * subdirectories as needed.
 * 4. Invoke `ConfigurationTreeBuilder.build(TopLevelType.class, pathToFile)` to
 * start recursive descent and create your bean.
 * 5. (Optional, Recommended) Use a validation framework to validate your
 * configuration.
 *
 * # Annotations
 *
 * This module provides the following annotations. They can be combined to create
 * arbitrarily deep or complex configuration trees.
 *
 * ## Bean
 *
 * Load a sibling file. Assuming an entry path at `/path/to/config.json`, an object
 * annotated with @Bean will look for its configured file in the same directory.
 *
 * ### Example:
 *
 * {@code
 * ```java
 * public class Config {
 *   @Bean
 *   ServerSettings server;
 * }
 * ```
 * }
 *
 * Given this setup, the file system may look like this:
 *
 * * /path/to/
 *   * config.json
 *   * server.json
 *
 * ## BeanCollection
 *
 * Load a directory tree. `BeanCollection` supports two mapping modes: a single
 * directory of files, or multiple subdirectories with a known entry point. The
 * default mapping is a single directory. This must be applied to List, Set, Queue,
 * or a Map type. When applied to a Map type, the key *must* be a String. The
 * derived name of the bean will be used as the Map key.
 *
 * ### Single Directory Mode
 *
 * Every file matching the expected extension in a subdirectory will be loaded.
 * This allows using a `.d` type mode where each file in the directory is expected
 * to follow a common schema. In this mode, beans are named following their file
 * name (without extension).
 *
 * #### Example:
 *
 * {@code
 * ```java
 * public class Config {
 *   @BeanCollection("plugin.d")
 *   List<Plugin> plugins;
 * }
 * ```
 * }
 *
 * Given this setup, the file system may look like this:
 *
 * * /path/to/
 *   * config.json
 *   * plugin.d/
 *     * foo.json
 *     * bar.json
 *
 * This would result in the `plugins` list having entries for each file,
 * deserialized using `Plugin` as the schema.
 *
 * ### Multi-Directory Mode
 *
 * Each subdirectory that contains a specifically named file will be loaded. This
 * allows using folders that contain multiple files in a coherent way. In this
 * mode, beans are named based on the subdirectory where they reside.
 *
 * #### Example:
 *
 * {@code
 * ```java
 * public class Config {
 *   @BeanCollection(value = "plugin", mapping = Mapping.MULTI_DIRS)
 *   Map<String, Plugin> plugins;
 * }
 * ```
 * }
 *
 * Given this setup, the file system may look like this:
 *
 * * /path/to/
 *   * config.json
 *   * foo/
 *     * plugin.json
 *   * bar/
 *     * plugin.json
 *   * baz/
 *     * not-plugin.json
 *
 * This would result in the `plugins` map being provided a `foo` and `bar` `Plugin`
 * loaded from those two subdirectories. The `baz` directory is not mapped in,
 * because it does not contain the `plugin.json` entry point.
 *
 * ## Template
 *
 * It is possible to apply templates (aka default configurations) as the bean tree
 * is loaded. A `Template` annotation can declare an in-line *or* external
 * template.
 *
 * One advantage of templates is that they become global, referenced by their name,
 * to the entire configuration tree. It may be convenient to define defaults at the
 * topmost level, and then freely re-use the declarations at any point in the bean
 * tree. However a template will not be *discovered* until the recursive descent
 * parser reaches the portion of the graph where it is defined, so they are
 * probably best defined at a high level anyway. (TODO: do a graph walk to find
 * templates first and eliminate this limitation?)
 *
 * ### In-Line Template
 *
 * {@code
 * ```java
 * public class Config {
 *   @Template("server")
 *   ServerSettings serverTemplate;
 *
 *   @Bean
 *   ServerSettings server;
 * }
 * ```
 * }
 *
 * In the above scenario, a `serverTemplate` key should be in the `config.json`,
 * which resolves to an object of the same schema as ServerSettings. When the
 * `server` bean is loaded, the `server` template is looked up (by default; this
 * name can be overridden with the `template` setting in `@Bean`), copied, and
 * used as default values for anything unspecified in `server.json`. In-line
 * templates are less useful for `Bean` annotations, because defaults can be
 * applied in-line without a `Template` by specifying values for `server` inside
 * `config.json` (in this case). However, in-line templates may have value for
 * `BeanCollection` scenarios.
 *
 * Sample `config.json`:
 *
 * ```json
 * {
 *   "serverTemplate" : {
 *     "port" : 8080,
 *     "basePath" : "/api"
 *   }
 * }
 * ```
 *
 * Sample `server.json`:
 *
 * ```json
 * {
 *   "port" : 8888
 * }
 * ```
 *
 * In this sample, `8888` overrides the template's `8080`, but the `ServerSettings`
 * bean inherits the `basePath` setting.
 *
 * ### External Template
 *
 * {@code
 * ```java
 * public class Config {
 *   @Template(value="serverDefaults", external = @Bean("defaults/server"))
 *   ServerSettings serverTemplate;
 *
 *   @Bean(value = "server", template = "serverDefaults")
 *   ServerSettings server;
 * }
 * ```
 * }
 *
 * The above configuration will load the defaults from a sibling file, in this case
 * `defaults/server.json`. The above example also shows explicit naming. The file
 * tree may look like this:
 *
 * * /path/to/
 *   * config.json
 *   * server.json
 *   * defaults/
 *     * server.json
 *
 * Templates don't *need* to be placed in a separate directory, but it may be
 * useful to do so if the bean in question also loads sibling files, as the bean
 * tree module is fully recursive.
 *
 * External templates can depend on templates, but this is probably best avoided.
 * If you wish to do this, declare the `template` setting in the `Bean` annotation
 * configured in the `external` setting. Template processing will be done in
 * dependency-order.
 *
 * Sample `defaults/server.json`:
 *
 * ```json
 * {
 *   "port" : 8080,
 *   "basePath" : "/api"
 * }
 * ```
 *
 * Sample `server.json`:
 *
 * ```json
 * {
 *   "port" : 8888
 * }
 * ```
 *
 * In this sample, `8888` overrides the template's `8080`, but the `ServerSettings`
 * bean inherits the `basePath` setting.
 *
 * #### Multi-directory BeanCollection with Template Example
 *
 * Re-using some structure from other examples, this is a sample structure of how
 * one might configure a template to use for multi-directory mappings of
 * `BeanCollection`.
 *
 * {@code
 * ```
 * public class Config {
 *   @Template(value = "default-plugin", external = @Bean("defaults/plugin/plugin"))
 *   Plugin defaultPlugin;
 *
 *   @BeanCollection(value = "plugin", mapping = Mapping.MULTI_DIR, template = "default-plugin")
 *   Map<String, Plugin> plugins;
 * }
 * ```
 * }
 *
 * Given this setup, the file system may look like this:
 *
 * * /path/to/
 *   * config.json
 *   * defaults/
 *   * plugin/
 *     * plugin.json
 *   * foo/
 *     * plugin.json
 *   * bar/
 *     * plugin.json
 *
 * Creating the `defaults/plugin/` subdirectory accomplishes 2 things:
 *
 * It prevents the `BeanCollection` annotation from catching the `defaults`
 * directory as a plugin. If it were just `defaults/plugin.json`, it would match,
 * and the Plugin map would then have 3 `Plugin` instances (`foo`, `bar`, and
 * `plugin`). Using a file name other than `plugin.json` would also solve this,
 * though.
 * It allows the `Plugin` class to cleanly declare its own sibling file `Bean`
 * members as part of its configuration graph.
 *
 * ## Name
 *
 * Both the `Bean` and `BeanCollection` annotations have a concept of a bean name,
 * based on the file or directory name resolution process. The `Name` annotation
 * must be applied to a String member, and this resolved name will be injected into
 * the bean.
 *
 * ## SourceFile
 *
 * Similar to `Name`, this annotation allows the source file Path or full name
 * (String) can be injected into your bean during the deserialization process.
 *
 * # Lifecycle
 *
 * When deserializing with Jackson:
 *
 * 1. Either a new instance of the target type is created, or cloned from a template.
 * 2. Using Jackson's deep merge capability, the instance is updated from the target
 *    file, with the mapper as configured by the client.
 * 3. Any bean tree annotations on the type are processed.
 *
 * There are 3 main phases of bean tree annotation processing in the final step
 * above:
 *
 * 1. Pre-processing - all `Template` annotations on the current type are processed.
 * 2. Beans - all `Bean` and `BeanCollection` annotations are processed, ordered by
 *    their configurable `index` parameter. It is at this point that recursion may
 *    occur, descending into child members of each type as needed.
 * 3. Post-processing - `Name` and `SourceFile` annotations are processed.
 *
 * This process recurses/repeats until the bean graph has been walked. It is driven
 * by bean annotations and not strictly by files in the file system.
 *
 * # Using TOML (or another syntax)
 *
 * It is possible to use any syntax supported by Jackson Databind. TOML is a great
 * fit for Jackson Bean Tree, because TOML is more human-friendly than JSON, and
 * avoids
 * [pitfalls with YAML](https://ruudvanasseldonk.com/2023/01/11/the-yaml-document-from-hell).
 * TOML, however, is not good at deep nesting. Jackson Bean Tree can take the place
 * of deep nesting in TOML.
 *
 * The following snippet shows how to configure to use TOML. It is helpful to avoid
 * specifying file extensions in the annotation configuration, to allow the
 * configured default file extension to be applied when searching for files.
 *
 * {@code
 * ```java
 * TomlMapper tomlMapper = ...; //get configured TOML mapper; Spring/Guice etc.
 * ConfigurationTreeBuilder builder = new ConfigurationTreeBuilder()
 *   .mapper(tomlMapper)
 *   .defaultExtension("toml");
 * ```
 * }
 *
 * This requires an additional dependency for `jackson-dataformat-toml`, which
 * provides the TOML mapper. At this point, the entire tree will be processed in
 * the same way, but by loading `*.toml` files and using the TOML language.
 **/
module net.hunterstrategy.jackson_config_tree {
    /*
     * Compile-only dependencies.
     */
    requires static com.github.spotbugs.annotations;

    /*
     * Jackson is exposed in the API, hence it should be transitive.
     */
    requires transitive com.fasterxml.jackson.databind;

    /*
     * Used for JavaBean name mangling (reflection)
     */
    requires java.desktop;

    /*
     * Allow Jackson to reflect into this package.
     */
    opens net.hunterstrategy.beantree to
            com.fasterxml.jackson.databind;

    /*
     * Public API packages.
     */
    exports net.hunterstrategy.beantree;
}
