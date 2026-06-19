plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        create("apiRoutes") {
            id = "com.hamon.apiroutes"
            implementationClass = "com.hamon.apiroutes.ApiRoutesPlugin"
        }
    }
}

dependencies {
    implementation("com.akuleshov7:ktoml-core:0.7.0")
    implementation("com.squareup:kotlinpoet:2.2.0")
    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}
