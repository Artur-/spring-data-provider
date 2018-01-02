package org.vaadin.artur.spring.dataprovider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToLongFunction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;

public class SpringDataProviderBuilder<T, F> {
    private final BiFunction<Pageable, F, Page<T>> queryFunction;
    private final ToLongFunction<F> lengthFunction;
    private final List<QuerySortOrder> defaultSortOrders = new ArrayList<>();

    private F defaultFilter = null;

    public SpringDataProviderBuilder(
            BiFunction<Pageable, F, Page<T>> queryFunction,
            ToLongFunction<F> lengthFunction) {
        this.queryFunction = queryFunction;
        this.lengthFunction = lengthFunction;
    }

    public SpringDataProviderBuilder<T, F> withDefaultSort(String column,
            SortDirection direction) {
        defaultSortOrders.add(new QuerySortOrder(column, direction));
        return this;
    }

    public SpringDataProviderBuilder<T, F> withDefaultFilter(F defaultFilter) {
        this.defaultFilter = defaultFilter;
        return this;
    }

    public DataProvider<T, F> build() {
        return new PageableDataProvider<T, F>() {
            @Override
            protected Page<T> fetchFromBackEnd(Query<T, F> query,
                    Pageable pageable) {
                return queryFunction.apply(pageable,
                        query.getFilter().orElse(defaultFilter));
            }

            @Override
            protected List<QuerySortOrder> getDefaultSortOrders() {
                return defaultSortOrders;
            }

            @Override
            protected int sizeInBackEnd(Query<T, F> query) {
                return (int) lengthFunction
                        .applyAsLong(query.getFilter().orElse(defaultFilter));
            }
        };
    }

    public ConfigurableFilterDataProvider<T, Void, F> buildFilterable() {
        return build().withConfigurableFilter();
    }

    public static <T> SpringDataProviderBuilder<T, Void> forRepository(
            PagingAndSortingRepository<T, ?> repository) {
        return new SpringDataProviderBuilder<>(
                (pageable, filter) -> repository.findAll(pageable),
                filter -> repository.count());
    }

    public static <T, F> SpringDataProviderBuilder<T, F> forFunctions(
            BiFunction<Pageable, F, Page<T>> queryFunction,
            ToLongFunction<F> lengthFunction) {
        return new SpringDataProviderBuilder<>(queryFunction, lengthFunction);
    }
}
