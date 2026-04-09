import { useSearchParams } from "react-router-dom";
import { UnifiedLoginForm } from "../../components/auth/UnifiedLoginForm";

export function MerchantLoginPage({ intent = "merchant", returnUrl = null }) {
  const [searchParams] = useSearchParams();
  const resolvedReturnUrl = returnUrl ?? searchParams.get("returnUrl");
  const registered = searchParams.get("registered") === "1";

  return (
    <div className="merchant-login-page">
      <UnifiedLoginForm
        intent={intent}
        returnUrl={resolvedReturnUrl}
        registered={registered}
        variant="page"
      />
    </div>
  );
}
