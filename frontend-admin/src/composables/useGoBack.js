import { useRouter } from "vue-router";

/**
 * useGoBack Composable
 *
 * Provides a professional back navigation behavior.
 * - If there is previous history in the application (excluding login/redirect loops), it navigates back,
 *   preserving search query, filters, pagination and sorting.
 * - If there is no previous history (e.g. refreshed, bookmarked, direct visit), it redirects to the fallback path.
 */
export function useGoBack() {
  const router = useRouter();

  const goBack = (fallbackPath = "/admin/dashboard") => {
    const backPath = window.history.state?.back;

    // Check if we have history.state.back and it's not a login-related path to prevent redirect loops.
    // Also check if we actually have history state to prevent unexpected external navigation.
    if (backPath && !backPath.includes("/login")) {
      router.back();
    } else {
      router.push(fallbackPath);
    }
  };

  return {
    goBack,
  };
}
