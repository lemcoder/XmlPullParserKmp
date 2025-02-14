plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "io.github.lemcoder"
version = "1.0.0"

kotlin {
    jvmToolchain(17)

    jvm {
        withJava()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    mingwX64()
    macosX64()
    macosArm64()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.fleeksoft.io.core)
                implementation(libs.fleeksoft.io.io)
                implementation(libs.fleeksoft.io.charset.ext)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation("com.goncalossilva:resources:0.10.0")
            }
        }

        jvmMain.dependencies {
            implementation("org.codehaus.plexus:plexus-xml:4.0.4") // Actual JVM implementation
        }

        jvmTest.dependencies {
            implementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
            implementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
        }
    }
}

tasks.withType<Test> {
    // check if target is jvm
    if (name.contains("jvm")) {
        maxHeapSize = "4g"
        forkEvery = 1
        maxParallelForks = Runtime.getRuntime().availableProcessors()
        useJUnitPlatform() // Enable JUnit 5
    }
}






