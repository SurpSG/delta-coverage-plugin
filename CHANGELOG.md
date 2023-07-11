# Delta-Coverage Gradle plugin Changelog

## 1.1.0
### Added
- Ignore coverage violation if a coverage entity count is less than threshold
  <details>
    <summary>Usage example</summary>
        
    ```kotlin
    // kotlin gradle dsl 
    configure<io.github.surpsg.deltacoverage.gradle.DeltaCoverageConfiguration> {
      violationRules {
          failOnViolation.set(true)
          
          rule(io.github.surpsg.deltacoverage.gradle.CoverageEntity.INSTRUCTION) {
              minCoverageRatio.set(0.9)
              entityCountThreshold.set(1234)
          }
          rule(io.github.surpsg.deltacoverage.gradle.CoverageEntity.LINE) {
              minCoverageRatio.set(0.8) 
              entityCountThreshold.set(567) 
          }
          rule(io.github.surpsg.deltacoverage.gradle.CoverageEntity.BRANCH) {
              minCoverageRatio.set(0.7)
              entityCountThreshold.set(890)
          }
      }
    }
        
    ```
    </details>

## 1.0.0
### Added
- `Delta Coverage` plugin applies JaCoCo plugin to a project and all it's subprojects
  - could be disabled by adding property to `gradle.properties`:
  ```
  io.github.surpsg.delta-coverage.auto-apply-jacoco=false
  ```
- Support of Gradle configuration cache
- Support of classes exclusion #69

### Changed
- Official support Gradle `8.1.+`.
- Updated JaCoCo dependency to [0.8.9](https://github.com/jacoco/jacoco/releases/tag/v0.8.9)
- Min supported Gradle is `5.1`.
- `compareWith` and `failIfCoverageLessThan` are changed to infix function: 
  - <details>
    <summary>Usage example</summary>
    
     ```kotlin
    // kotlin gradle dsl 
    configure<io.github.surpsg.deltacoverage.gradle.DeltaCoverageConfiguration> {
        // both are correct
        diffSource.git compareWith "HEAD"
        diffSource.git.compareWith("HEAD")
        
        // both are correct
        violationRules failIfCoverageLessThan 0.7
        violationRules.failIfCoverageLessThan(0.7)
    }
    
    ```
    </details>
- `deltaCoverage` task depends on `classes` tasks.
- Replaced the plugin extension values with [properties](https://docs.gradle.org/current/userguide/lazy_configuration.html)
