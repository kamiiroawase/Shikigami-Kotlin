plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    application
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(platform("com.aallam.openai:openai-client-bom:4.1.0"))
    implementation("com.aallam.openai:openai-client")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:10.0.0")
    implementation("org.commonmark:commonmark:0.29.0")
    implementation("org.apache.commons:commons-text:1.15.0")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation(files("libs/markdown2tg-2.1.4.jar"))
    runtimeOnly("io.ktor:ktor-client-okhttp")
}

application {
    mainClass = "com.shikigami.kotlin.base.App"
}

kotlin {
    jvmToolchain(21)
}

tasks.named<JavaExec>("run") {
    jvmArgs("-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8")
}
