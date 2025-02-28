# первый (вспомогательный) этап с именем builder
FROM amazoncorretto:22-alpine as builder
# устанавливаем application в качестве рабочей директории
WORKDIR application
# копируем артефакт в папку application в контейнере
COPY target/*.jar app.jar
# используем специальный режим запуска Spring Boot приложения,
# который активирует распаковку итогового jar-файла на составляющие
RUN java -Djarmode=layertools -jar app.jar extract

# заключительный этап, создающий финальный образ
FROM amazoncorretto:22-alpine
# поочерёдно копируем необходимые для приложения файлы,
# которые были распакованы из артефакта на предыдущем этапе;
# при этом каждая инструкция COPY создаёт новый слой
COPY --from=builder /application/dependencies/ ./
COPY --from=builder /application/spring-boot-loader/ ./
COPY --from=builder /application/snapshot-dependencies/ ./
COPY --from=builder /application/application ./
# в качестве команды указываем запуск специального загрузчика
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]