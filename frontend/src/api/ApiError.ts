/** A failure the server described with a stable code, which the UI turns into French. */
export class ApiError extends Error {
  constructor(
    readonly code: string,
    readonly status: number,
    readonly detail?: string,
  ) {
    super(detail ?? code)
    this.name = 'ApiError'
  }
}
