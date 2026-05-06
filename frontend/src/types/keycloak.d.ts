declare module 'keycloak-js' {
  interface KeycloakTokenParsed {
    department?: string[]
    resource_access?: {
      [clientId: string]: {
        roles: string[]
      }
    }
  }
}
