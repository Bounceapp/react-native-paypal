import { defineConfig, devices } from "@playwright/test"

const port = Number(process.env.WEB_PORT ?? 8082)

export default defineConfig({
  testDir: "./e2e/web",
  timeout: 60_000,
  retries: process.env.CI ? 1 : 0,
  forbidOnly: !!process.env.CI,
  use: {
    baseURL: `http://localhost:${port}`,
    ...devices["Pixel 7"],
    trace: "retain-on-failure",
  },
  // Reuses a running `expo start --web` on the same port, or starts one.
  webServer: {
    command: `pnpm exec expo start --web --port ${port}`,
    url: `http://localhost:${port}`,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
})
