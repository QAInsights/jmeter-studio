# JMeter Studio

A modern, extensible theme engine for Apache JMeter featuring both dark and light themes with custom icon sets.

## Features

- **Aura Dark Theme**: A sleek dark theme with purple accents for comfortable low-light usage
- **Aura Light Theme**: A clean light theme with purple accents for bright environments
- **Custom Icon Sets**: Professionally designed icons for both themes
- **Seamless Integration**: Works as a JMeter plugin with easy theme switching
- **Persistent Preferences**: Your theme choice is saved across JMeter sessions

## Requirements

- Apache JMeter 5.6.3 or later
- Java 17 or later

## Installation

### Manual Installation

1. Download the latest `aura-theme-1.0.0-SNAPSHOT.jar` from the releases page
2. Copy the JAR file to your JMeter installation's `lib/ext` directory:
   ```
   C:\Users\<username>\tools\apache-jmeter-5.6.3\lib\ext\
   ```
3. Copy the required dependencies to your JMeter `lib` directory:
   - `flatlaf-3.5.4.jar`
   - `gson-2.11.0.jar`
4. Restart JMeter

### Building from Source

```bash
# Clone the repository
git clone https://github.com/QAInsights/jmeter-studio.git
cd jmeter-studio

# Build the project
mvn clean package -DskipTests

# The plugin JAR will be available at:
# oss/target/aura-theme-1.0.0-SNAPSHOT.jar
```

## Usage

### Selecting a Theme

1. Open JMeter
2. Go to **Options** → **Theme**
3. Select either **Aura Dark** or **Aura Light**
4. JMeter will apply the theme immediately

### Switching Back to Default

1. Go to **Options** → **Theme** → **Default**
2. Restart JMeter to restore the original JMeter theme

### Building

```bash
# Compile and package
mvn clean package

# Run tests
mvn test

# Install to local repository
mvn install
```
## Acknowledgments

- Built with [FlatLaf](https://www.formdev.com/flatlaf/) - Modern Look and Feel for Java Swing
- Icons designed specifically for the Aura Theme
- Inspired by the JMeter community's need for modern UI themes

---

Made with ❤️ by QAInsights
