/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: MIT
 * For full license text, see the LICENSE file in the repo root
 * or https://opensource.org/licenses/MIT
 */
package utam.compiler.translator;

import static utam.compiler.translator.DefaultTranslatorConfiguration.getConfiguredProfiles;
import static utam.compiler.translator.DefaultTranslatorConfiguration.getScanner;
import static utam.compiler.translator.DefaultTranslatorConfiguration.getScannerConfig;
import static utam.core.declarative.translator.GuardrailsMode.WARNING;
import static utam.core.declarative.translator.UnitTestRunner.NONE;
import static utam.core.declarative.translator.UnitTestRunner.validateUnitTestDirectory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import utam.core.declarative.translator.GuardrailsMode;
import utam.core.declarative.translator.TranslatorConfig;
import utam.core.declarative.translator.TranslatorRunner;
import utam.core.declarative.translator.TranslatorSourceConfig;
import utam.core.declarative.translator.TranslatorTargetConfig;
import utam.core.declarative.translator.UnitTestRunner;

@Command(name = "generatePageObjects", mixinStandardHelpOptions = true, description = "A command to generate UTAM Page Objects")
public class TranslatorGenerationCommand implements Callable<Integer> {

  static final int CONFIG_ERR = ExitCode.USAGE;
  static final int RUNTIME_ERR = ExitCode.SOFTWARE;
  static final String MISSING_INPUT =
      "You must specify an input directory with the --inputDirectory argument or a list of files";
  static final String TOO_MANY_INPUTS =
      "You cannot specify both an input directory with the --inputDirectory argument and a list of files";
  static final String INVALID_UNIT_TEST_CONFIG =
      "You cannot specify a unit test runner without a destination directory for unit tests";
  static final String OUTPUT_DIRECTORY_MISSING = "Output directory is not configured";
  static final String PACKAGE_CONFIG_MISSING = "Packages mapping is not configured";
  static final String REDUNDANT_CLI_ARGS = "If JSON file is set, all other arguments are ignored";
  static final String ERR_COMPILER_CONFIG_NEEDS_ROOT = "To resolve configuration paths, set compilerRoot";

  @Option(names = {"-f", "-config", "--config"},
      description = "JSON file with configuration. "
          + "Path should be relative to the current module. "
          + "When set, all other command line parameters will be ignored!")
  File jsonConfig;

  @Option(names = {"-c", "-compilerRoot", "--compilerRoot"},
      description = "Root folder for compiler to resolve relative paths")
  File compilerRoot;

  @Option(names = {"-o", "-outputDirectory", "--outputDirectory"},
      description = "Output directory to which generated Page Object files will be written.")
  File outputDirectory;

  @Option(names = {"-m", "-packageMappingFile", "--packageMappingFile"},
      description = "File containing mapping between directories and package names.")
  File packageMappingFile;

  @Option(names = {"-r", "-unitTestRunner", "--unitTestRunner"},
      description = "Unit test runner to use for generated unit tests for Page Objects. Valid values: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
  UnitTestRunner testRunner;

  @Option(names = {"-i", "-inputDirectory", "--inputDirectory"},
      description = "Input directory to be recursively scanned for UTAM declarative Page Object description files. Cannot be used with an explicit file list.")
  File inputDirectory;

  @Parameters(description = "Explicit list of UTAM declarative Page Object description files to generate. Cannot be used with the --inputDirectory option.")
  List<File> inputFiles;

  @Option(names = {"-p", "-profileDirectory", "--profileDirectory"},
      description = "Destination directory to which profile information will be written.")
  private File profileDirectory;

  @Option(names = {"-d", "-profileDefinitionsFile", "--profileDefinitionsFile"},
      description = "File containing definitions of profile names and their valid values.")
  private File profileDefinitionsFile;

  @Option(names = {"-u", "-unitTestDirectory", "--unitTestDirectory"},
      description = "Destination directory to which generated unit tests will be written.")
  private File unitTestDirectory;

  @Option(names = {"-n", "-moduleName", "--moduleName"},
      description = "Name of the current POs module, when set it's used as a prefix to profile property files.")
  private String moduleName;

  @Option(names = {"-g", "-guardrails", "--guardrails"},
      description = "Defines how strict should be guardrails violations, possible values: 'error' or 'warning'")
  private String validationStrict;

  private Exception thrownError;
  Integer returnCode = CommandLine.ExitCode.OK;

  public Exception getThrownError() {
    return thrownError;
  }

  private TranslatorConfig setConfigFromJSON() {
    if (outputDirectory != null
        || packageMappingFile != null
        || testRunner != null
        || inputFiles != null
        || inputDirectory != null
        || profileDirectory != null
        || profileDefinitionsFile != null
        || unitTestDirectory != null
        || moduleName != null) {
      thrownError = new UnsupportedOperationException(REDUNDANT_CLI_ARGS);
      returnCode = CONFIG_ERR;
      return null;
    }
    if (compilerRoot == null) {
      thrownError = new NullPointerException(ERR_COMPILER_CONFIG_NEEDS_ROOT);
      returnCode = CONFIG_ERR;
      return null;
    }
    try {
      JsonCompilerConfig jsonConfig = new JsonCompilerConfig(this.jsonConfig, this.compilerRoot);
      return jsonConfig.getTranslatorConfig(WARNING);
    } catch (IOException e) {
      thrownError = e;
      returnCode = RUNTIME_ERR;
      return null;
    }
  }

  private TranslatorConfig setConfigFromCommandLineArgs() {
    try {
      if (inputDirectory == null && (inputFiles == null || inputFiles.size() == 0)) {
        // Must specify either an input directory or a list of files
        thrownError = new UnsupportedOperationException(MISSING_INPUT);
        returnCode = CONFIG_ERR;
        return null;
      }

      if (inputDirectory != null && inputFiles != null && inputFiles.size() > 0) {
        // Cannot specify both input directory and list of files.
        thrownError = new UnsupportedOperationException(TOO_MANY_INPUTS);
        returnCode = CONFIG_ERR;
        return null;
      }

      if(testRunner == null) {
        testRunner = NONE;
      }

      if (testRunner != NONE && unitTestDirectory == null) {
        // If specifying a unit test runner, you must specify a unit test directory.
        thrownError = new UnsupportedOperationException(INVALID_UNIT_TEST_CONFIG);
        returnCode = CONFIG_ERR;
        return null;
      }

      String unitTestDirectoryPath = "";
      if (unitTestDirectory != null) {
        unitTestDirectoryPath = validateUnitTestDirectory(testRunner, unitTestDirectory.toString());
      }

      if (outputDirectory == null) {
        thrownError = new UnsupportedOperationException(OUTPUT_DIRECTORY_MISSING);
        returnCode = CONFIG_ERR;
        return null;
      }

      TranslatorTargetConfig targetConfig = new DefaultTargetConfiguration(
          outputDirectory.toString(),
          profileDirectory == null ? "" : profileDirectory.toString(),
          testRunner,
          unitTestDirectoryPath
      );

      if (packageMappingFile == null) {
        thrownError = new UnsupportedOperationException(PACKAGE_CONFIG_MISSING);
        returnCode = CONFIG_ERR;
        return null;
      }

      TranslatorSourceConfig sourceConfig = new DefaultSourceConfiguration(
          getScannerConfig(packageMappingFile),
          getScanner(inputDirectory, inputFiles));

      GuardrailsMode guardrailsMode = validationStrict == null? WARNING : GuardrailsMode.valueOf(validationStrict.toUpperCase());

      return new DefaultTranslatorConfiguration(
          moduleName,
          guardrailsMode,
          sourceConfig,
          targetConfig,
          getConfiguredProfiles(profileDefinitionsFile));

    } catch (IOException e) {
      thrownError = e;
      returnCode = RUNTIME_ERR;
      return null;
    }
  }

  TranslatorConfig getTranslationConfig() {
    if (jsonConfig != null) {
      return setConfigFromJSON();
    }
    return setConfigFromCommandLineArgs();
  }

  @Override
  public Integer call() {
    TranslatorConfig translatorConfig = getTranslationConfig();
    if (translatorConfig == null) {
      return returnCode; //error during configuration, exit
    }
    try {
      TranslatorRunner translator = new DefaultTranslatorRunner(translatorConfig);
      translator.run();
      translator.write();
      translator.writeDependenciesConfigs();
      returnCode = CommandLine.ExitCode.OK;
    } catch (IOException e) {
      thrownError = e;
      returnCode = RUNTIME_ERR;
    }
    return returnCode;
  }
}

