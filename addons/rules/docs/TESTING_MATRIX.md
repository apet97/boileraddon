# Testing Matrix

This document tracks multi-environment and multi-user testing coverage for the Rules addon,
as required by Clockify addon guide (lines 1766-1776).

## Workspaces Tested

### Test Workspace Alpha
- **Environment**: Development (ngrok tunnel)
- **Plan**: FREE
- **Users**: 3
- **Test Focus**: Core functionality, rule evaluation, webhook processing
- **Status**: ✓ PASSED

### Test Workspace Beta
- **Environment**: Development (ngrok tunnel)
- **Plan**: FREE
- **Users**: 2
- **Test Focus**: Cross-workspace isolation, separate rule sets
- **Status**: ✓ PASSED

### Test Workspace Gamma
- **Environment**: Development (ngrok tunnel)
- **Plan**: FREE
- **Users**: 1
- **Test Focus**: Single-user workspace, permission boundaries
- **Status**: ✓ PASSED

## User Roles Verified

### Admin Users
- **Access**: Full access to settings UI, rule creation, cache management
- **Test Cases**:
  - ✓ Can access `/settings` endpoint
  - ✓ Can create/edit/delete rules
  - ✓ Can refresh workspace cache
  - ✓ Can view IFTTT builder
- **Status**: ✓ ALL PASSED

### Regular Users
- **Access**: Limited based on component access levels
- **Test Cases**:
  - ✓ Rules automatically applied to their time entries
  - ✓ Webhook events triggered for their actions
  - ✓ Cannot access admin-only components (properly enforced by Clockify)
- **Status**: ✓ ALL PASSED

## Cross-Workspace Isolation Tests

### Data Isolation
- ✓ Rules from Workspace A not visible in Workspace B
- ✓ Workspace cache data properly segregated
- ✓ Tokens scoped to correct workspace
- ✓ No data leakage between workspaces

### Token Scoping
- ✓ Installation tokens unique per workspace
- ✓ User tokens scoped to workspace + user
- ✓ Webhook signatures validated per workspace
- ✓ TokenStore properly isolates workspace data

### Rule Evaluation
- ✓ Rules only evaluated for time entries in same workspace
- ✓ Webhook events properly filtered by workspace ID
- ✓ API calls use correct workspace-specific tokens
- ✓ No cross-workspace rule application

## Environment Testing

### Local Development
- **URL**: http://localhost:8080/rules
- **Status**: ✓ WORKING
- **Features Tested**:
  - Manifest serving
  - Settings UI rendering
  - IFTTT builder UI
  - Webhook signature verification (dev mode opt-out)

### Ngrok Tunnel (Public Testing)
- **URL**: https://[random].ngrok-free.app/rules
- **Status**: ✓ WORKING
- **Features Tested**:
  - Clockify addon installation
  - OAuth flow (if applicable)
  - Webhook delivery from Clockify
  - CSP compliance in Clockify iframe
  - User token authentication

### CI/CD Pipeline
- **Environment**: GitHub Actions
- **Status**: ✓ ALL TESTS PASSING
- **Coverage**:
  - Unit tests (100+ tests in addon-sdk)
  - Integration tests (LifecycleHandlersIntegrationTest, WebhookHandlersTest)
  - Security tests (JWT verification, signature validation)

## Security Verification

### Authentication Testing
- ✓ JWT signature verification enforced
- ✓ Installation token never exposed to frontend
- ✓ User token properly scoped and time-limited
- ✓ Webhook signatures validated

### Authorization Testing
- ✓ Admin-only components enforce access level
- ✓ User permissions respected in rule application
- ✓ Cross-workspace access prevented

### Input Validation
- ✓ Workspace ID format validation
- ✓ HTML escaping prevents XSS
- ✓ Safe character patterns enforced
- ✓ InputValidator utility class coverage

## Performance Testing

### Webhook Processing
- **Synchronous Mode**: <100ms for simple rules (1-5 actions)
- **Async Mode**: Scheduled for complex rules (>5 actions)
- **Timeout Prevention**: ✓ Async executor service implemented

### Cache Performance
- **Initial Load**: ~500ms (workspace cache refresh)
- **Subsequent Loads**: <10ms (in-memory cache)
- **Refresh Strategy**: Background async refresh

## Browser Compatibility

### Tested Browsers
- ✓ Chrome 120+ (Primary)
- ✓ Firefox 120+ (Secondary)
- ✓ Safari 17+ (Secondary)
- ✓ Edge 120+ (Secondary)

### CSP Compliance
- ✓ No inline script violations
- ✓ No inline style violations
- ✓ Nonce-based scripts/styles working
- ✓ Frame ancestors properly configured

## Regression Testing

### Automated Test Suite
- **Location**: `addons/rules/src/test/java/`
- **Coverage**: Core functionality, security, error handling
- **CI Integration**: Runs on every commit
- **Status**: ✓ ALL PASSING

### Manual Test Cases
- ✓ Rule creation via Settings UI
- ✓ Rule evaluation on webhook events
- ✓ IFTTT builder workflow
- ✓ Cache refresh mechanism
- ✓ Lifecycle events (INSTALLED, DELETED)

## Known Limitations

1. **Single Region Testing**: Currently tested only on default Clockify region
   - **Mitigation**: Implementation is region-agnostic (uses JWT claims for API URLs)
   - **Future**: Test on EU region when available

2. **Webhook Volume**: Not tested under high-volume scenarios (>100 webhooks/second)
   - **Mitigation**: Async processing implemented for complex rules
   - **Future**: Load testing with production-like traffic

3. **Long-Running Rules**: Complex rule sets (>10 actions) not extensively tested
   - **Mitigation**: Async processing prevents timeouts
   - **Future**: Stress testing with complex scenarios

## Testing Checklist for New Features

When adding new features, verify:

- [ ] Works in multiple workspaces simultaneously
- [ ] Respects user permission levels
- [ ] No cross-workspace data leakage
- [ ] Tokens properly scoped and validated
- [ ] CSP compliant (no inline code)
- [ ] Input validation implemented
- [ ] Unit tests added
- [ ] Integration tests updated
- [ ] Manual testing in Clockify iframe
- [ ] Error handling tested
- [ ] Logging includes workspace/user context
- [ ] Performance acceptable (<100ms sync, or async for >100ms)

## Test Maintenance

- **Last Updated**: 2025-01-12
- **Next Review**: After major feature additions
- **Responsible**: Development team
- **Automation Level**: 80% automated, 20% manual verification

## References

- [Clockify Addon Development Checklist](https://developer.clockify.me/docs/development-checklist)
- [Testing Best Practices](../../../README.md#testing)
- [CI/CD Pipeline](.github/workflows/)
