import * as React from "react"
import { Pressable, StyleSheet } from "react-native"

import type { PaypalButtonProps } from "../types"
import PaypalLogo from "./PaypalLogo"

const PaypalButton = ({
  style,
  disabled = false,
  onPress,
}: PaypalButtonProps) => (
  <Pressable
    onPress={onPress}
    disabled={disabled}
    accessibilityRole="button"
    style={({ pressed }) => [
      styles.idle,
      pressed && styles.pressed,
      disabled && styles.disabled,
      style,
    ]}>
    {!disabled && <PaypalLogo height={22} width={69} />}
  </Pressable>
)

const styles = StyleSheet.create({
  idle: {
    height: 48,
    width: "100%",
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#FFC439",
    borderRadius: 6,
  },
  pressed: {
    backgroundColor: "#F2BA37",
  },
  disabled: {
    backgroundColor: "#E5E7EB",
  },
})

export default PaypalButton
