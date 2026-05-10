import type { Role } from "../types";

export const CLIENT: Role = "CLIENT";
export const OPERATOR: Role = "OPERATOR";

export function hasAnyRole(userRole: Role | undefined, allowedRoles?: Role[]): boolean {
  if (!allowedRoles || allowedRoles.length === 0) {
    return true;
  }
  return Boolean(userRole && allowedRoles.includes(userRole));
}

export function roleLabel(role?: Role): string {
  if (role === "CLIENT") {
    return "Client";
  }
  if (role === "OPERATOR") {
    return "Operator";
  }
  return "Unknown";
}
