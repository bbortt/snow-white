module.exports = {
  endOfLine: 'lf',
  printWidth: 80,
  singleQuote: true,
  tabWidth: 2,
  useTabs: false,
  plugins: ['@prettier/plugin-xml', 'prettier-plugin-java'],
  overrides: [
    {
      files: '*.xml',
      options: {
        parser: 'xml',
      },
    },
    {
      files: '*.java',
      options: {
        parser: 'java',
      },
    },
    {
      files: '*.json',
      options: {
        trailingComma: 'none',
      },
    },
    {
      files: '**/CHANGELOG.md',
      options: {
        requirePragma: true,
      },
    },
  ],
};
