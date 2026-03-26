# E-Commerce Checkout: Authenticate, Lookup, Cart, Checkout

A customer wants to buy a product. The request must authenticate the user, look up the
product in the catalog, add it to the cart, and process checkout -- four services that need
to execute in sequence with data flowing from each to the next.

## Workflow

```
userId, productId, quantity
            |
            v
+--------------------+     +----------------------+     +-------------------+     +------------------+
| so_authenticate    | --> | so_catalog_lookup    | --> | so_add_to_cart    | --> | so_checkout      |
+--------------------+     +----------------------+     +-------------------+     +------------------+
  token: jwt-token-abc      product details              cartId: CART-7891       orderId:
  authenticated: true       (name, price, stock)         total: 79.99*qty        ORD-20240301-001
                                                                                  status: confirmed
                                                                                  delivery: 2024-03-05
```

## Workers

**AuthenticateWorker** -- Authenticates `userId`. Returns `token: "jwt-token-abc123"`,
`authenticated: true`.

**CatalogLookupWorker** -- Looks up `productId` in the catalog. Returns product details
(name, price, stock availability).

**AddToCartWorker** -- Adds `quantity` items to the cart. Returns `cartId: "CART-7891"`,
`items: 1`, `total: 79.99 * quantity`.

**CheckoutWorker** -- Processes checkout for the cart. Returns
`orderId: "ORD-20240301-001"`, `orderStatus: "confirmed"`,
`estimatedDelivery: "2024-03-05"`.

## Tests

32 unit tests cover authentication, catalog lookup, cart operations, and checkout.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
