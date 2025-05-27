<h1 align="center">Snow-White 🍎</h1>

<p align="center">
    <img src=".github/logo.png" alt="Snow-White Logo" width="100px" />
</p>

<p align="center">
    <i>An awesome pairing with, well.. do you know Snow White and the Jaeger?</i>
</p>

Snow-White makes API testing effortless by leveraging API specifications and [OpenTelemetry (OTEL)](https://opentelemetry.io/) data.
OTEL is a standardized protocol that allows flexible data sourcing.

Most commonly, Snow-White listens to **black-box tests of your application**, such as system or integration tests.
However, it can also gather insights from a **live production environment**.

Snow-White provides valuable insights on:

- Coverage
- API Performance
- And more.

## Development & Contributing

We welcome contributions!
Whether you're fixing a bug, adding a feature, or improving the documentation – thank you 🙌

To get started:

1. Read our [`DEVELOPMENT.md`](./DEVELOPMENT.md) for a step-by-step guide on setting up your local environment.
2. Open a Pull Request or file an issue if you’ve found something worth improving.
3. Make sure to follow the coding style and commit conventions described in [`DEVELOPMENT.md`](./DEVELOPMENT.md).

### Local Setup (Quick Start)

```shell
git clone https://github.com/bbortt/snow-white.git
cd snow-white

# Download dependencies and pack application
./mvnw -Pnode package
```

### Contributing Guidelines

<!-- TODO: Add these -->

Please review our contributing guide and code of conduct before submitting a PR.

## License

This project is licensed under the [Polyform Small Business License 1.0.0](https://polyformproject.org/licenses/small-business/1.0.0/).

This means you can freely use, modify, and distribute the software **if your company generates less than $1 million USD in annual revenue** and meets other conditions in the license.

For companies exceeding this threshold or for commercial/production use, a commercial license is required. Contact me at [timon.borter@gmx.ch] for more information.

See the [LICENSE](./LICENSE) file for full details.
