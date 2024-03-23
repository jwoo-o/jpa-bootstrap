package persistence.entity.binder;

import java.util.List;
import java.util.stream.Collectors;
import persistence.sql.ComponentScanner;

public class AnnotationBinder {

    private AnnotationBinder() {
    }

    public static List<Class<?>> bind(String basePackage) {
        return ComponentScanner.getClasses(basePackage).stream()
            .filter(clazz -> clazz.isAnnotationPresent(jakarta.persistence.Entity.class))
            .collect(Collectors.toList());
    }
}
