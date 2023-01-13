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
package net.hunterstrategy.beantree;


import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.hunterstrategy.beantree.analysis.ConfigAnalyzerCache;
import net.hunterstrategy.beantree.analysis.DeserializationContext;

public class ConfigurationTreeBuilder {
    private ObjectMapper mapper;
    private ConfigAnalyzerCache cache;
    private String defaultExtension = ".json";
    private Map<Class<?>, Supplier<?>> factories = new ConcurrentHashMap<>();

    private DeserializationContext context;

    public ConfigurationTreeBuilder() {
        defaultCollectionFactories();
    }

    private void defaultCollectionFactories() {
        factories.put(Map.class, ConcurrentHashMap::new);
        factories.put(List.class, ArrayList::new);
        factories.put(Set.class, HashSet::new);
        factories.put(Queue.class, ArrayDeque::new);
    }

    private void possiblyInstantiateMapper(boolean withDefaultSettings) {
        if (mapper == null) {
            this.mapper = new ObjectMapper();
            if (withDefaultSettings) {
                defaultMapperVisibilitySettings();
            }
        }
    }

    private void possiblyInstantiateDependencies() {
        possiblyInstantiateMapper(true);
        if (cache == null) {
            this.cache = new ConfigAnalyzerCache();
        }
        if (context == null) {
            this.context = new DeserializationContext(mapper, cache, defaultExtension, factories);
        }
    }

    /**
     * Configure the Jackson mapper with the default visibility settings:
     *
     * * Any field
     * * Any public setter
     */
    public ConfigurationTreeBuilder defaultMapperVisibilitySettings() {
        possiblyInstantiateMapper(false);
        this.mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        this.mapper.setVisibility(PropertyAccessor.SETTER, Visibility.PUBLIC_ONLY);
        return this;
    }

    /**
     * Specify an ObjectMapper to use. This can be configured any way
     * necessary to support deserializing your configuration tree.
     *
     * @param mapper the Jackson ObjectMapper to use
     */
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Intended behavior to allow client to pass in Mapper.")
    public ConfigurationTreeBuilder mapper(ObjectMapper mapper) {
        this.mapper = mapper;
        return this;
    }

    /**
     * Re-use the analysis cache from another ConfigurationTreeBuilder
     * instance. In the case where classes are being re-used, the analysis
     * steps can be skipped to accelerate deserialization.
     *
     * @param other the previously-executed ConfigurationTreeBuilder
     */
    public ConfigurationTreeBuilder reuseCache(ConfigurationTreeBuilder other) {
        this.cache = other.cache;
        this.context = null; // force context to be recreated
        return this;
    }

    /**
     * Re-use the bean factories configured from another ConfigurationTreeBuilder
     * instance. This is just a convenience to shorten the amount of configuration
     * that needs to be set up per-instance.
     *
     * @param other the previously created ConfigurationTreeBuilder
     */
    public ConfigurationTreeBuilder reuseFactories(ConfigurationTreeBuilder other) {
        this.factories.putAll(other.factories);
        this.context = null; // force context to be recreated
        return this;
    }

    /**
     * Use a specific implementation to create a bean type, rather than trying
     * to find a no-argument public constructor. This can also influence which collection
     * types to use when they are specified by their interface names - Map, List, Set, Queue.
     *
     * This only influences Jackson Bean Tree, and *not* Jackson itself. Configure
     * the ObjectMapper as necessary to influence its deserialization process.
     */
    public <BEAN> ConfigurationTreeBuilder factory(Class<BEAN> type, Supplier<BEAN> factory) {
        factories.put(type, factory);
        return this;
    }

    /**
     * Set the default extension, in the case where annotations don't specify
     * a file extension (or a file name at all). Defaults to ".json" Also used
     * for glob matching when scanning directories.
     *
     * @param extension the file extension to use
     */
    public ConfigurationTreeBuilder defaultExtension(String extension) {
        this.defaultExtension = extension.startsWith(".") ? extension : String.format(".%s", extension);
        this.context = null; // force context to be recreated
        return this;
    }

    public <T> T build(Class<T> type, Path configurationFile) {
        possiblyInstantiateDependencies();

        try {
            return context.deserialize(type, configurationFile);
        } finally {
            this.context = null; // clear any state from deserialization
        }
    }
}
