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
    implementation("com.akuleshov7:ktoml-core:0.5.5")
    implementation("com.squareup:kotlinpoet:1.18.1")
    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}
