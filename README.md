# Konfy
- YAML utility toolkit based on `kaml` library

## Features
- We wrapped yaml parser in this library
- You can directly access Konfy API to load yaml or save object to yaml format
- also allow you upgrade yaml config from older yaml with `ConfigTransformer`

## Config Transformer
- This interface allow you to implement `transform` function to transforming YAML Data
- In transformer, we use `YamlWrapper` to quickly access config section to get/set data


## Maven

- Repository
```kotlin
repositories {
    maven("https://repo.fastmcmirror.org/content/repositories/releases/")
}
```

- Dependency
```kotlin
dependencies {
    implementation("dev.rgbmc:konfy:$version-$shortGitHash")
}
```