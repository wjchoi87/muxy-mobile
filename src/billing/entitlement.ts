export type Entitlement =
  | { kind: 'loading' }
  | { kind: 'trial'; msRemaining: number }
  | { kind: 'expired' }
  | { kind: 'unlocked' };

export const TRIAL_DURATION_MS = 3 * 24 * 60 * 60 * 1000;

export const PAYMENT_REQUIRED = false;

export type ComputeArgs = {
  purchased: boolean;
  trialStartedAt: number | null;
  now: number;
};

export function computeEntitlement(args: ComputeArgs): Entitlement {
  if (args.purchased) return { kind: 'unlocked' };
  if (!PAYMENT_REQUIRED) return { kind: 'trial', msRemaining: TRIAL_DURATION_MS };
  if (args.trialStartedAt == null) return { kind: 'loading' };
  const remaining = TRIAL_DURATION_MS - (args.now - args.trialStartedAt);
  return remaining > 0 ? { kind: 'trial', msRemaining: remaining } : { kind: 'expired' };
}

export function daysRemaining(msRemaining: number): number {
  return Math.max(1, Math.ceil(msRemaining / (24 * 60 * 60 * 1000)));
}
