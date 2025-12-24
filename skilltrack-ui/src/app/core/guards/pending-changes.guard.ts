import { CanDeactivateFn } from '@angular/router';

/**
 * Components can implement this interface to participate in the unsaved-changes warning.
 */
export interface CanComponentDeactivate {
  hasUnsavedChanges(): boolean;
}

/**
 * CanDeactivate guard:
 * - If the component says it has unsaved changes, confirm navigation.
 * - Keeps accidental navigation from losing form edits.
 */
export const pendingChangesGuard: CanDeactivateFn<CanComponentDeactivate> = (component) => {
  try {
    return component?.hasUnsavedChanges?.() ? window.confirm('You have unsaved changes. Leave this page?') : true;
  } catch {
    return true;
  }
};
