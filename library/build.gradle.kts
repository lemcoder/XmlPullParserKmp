plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "io.github.lemcoder"
version = "1.0.0"

kotlin {
    jvmToolchain(11)

    jvm {
        withJava()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                //put your multiplatform dependencies here
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
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
    useJUnitPlatform() // Enable JUnit 5
}






