import { expect, test, type Page } from "@playwright/test"

// PayPal has no web SDK here: src/Paypal.web.ts resolves every request with a
// Failed error. These tests guard that a web app importing the package renders
// and gets that error instead of crashing.

const status = (page: Page) => page.getByTestId("status")

const open = async (page: Page) => {
  const errors: string[] = []
  page.on("pageerror", error => errors.push(error.message))
  page.on("console", message => {
    if (message.type() === "error") {
      errors.push(message.text())
    }
  })
  await page.goto("/")
  await expect(status(page)).toHaveText("idle", { timeout: 30_000 })
  return errors
}

test("renders without errors", async ({ page }) => {
  const errors = await open(page)
  await expect(page.getByTestId("pay")).toBeVisible()
  expect(errors).toEqual([])
})

test("resolves a request with the web error", async ({ page }) => {
  const errors = await open(page)
  await page.getByTestId("pay").click()
  await expect(status(page)).toHaveText("pay:Failed")
  await expect(
    page.getByText(
      /pay #1 Failed: PayPal billing agreements are not supported on web$/,
    ),
  ).toBeVisible()
  expect(errors).toEqual([])
})

test("resolves concurrent requests independently", async ({ page }) => {
  await open(page)
  await page.getByTestId("concurrent").click()
  await expect(status(page)).toHaveText("concurrent:Failed,Failed")
})
