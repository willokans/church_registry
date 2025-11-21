# Frontend Development Guide - Church Registry API

This document provides comprehensive information for building a Next.js frontend application that integrates with the Church Registry backend API.

## Table of Contents

1. [API Base Configuration](#api-base-configuration)
2. [Authentication](#authentication)
3. [API Endpoints](#api-endpoints)
4. [Data Transfer Objects (DTOs)](#data-transfer-objects-dtos)
5. [Error Handling](#error-handling)
6. [TypeScript Types](#typescript-types)
7. [Next.js Integration Guide](#nextjs-integration-guide)
8. [Example Code](#example-code)

---

## API Base Configuration

### Base URL

- **Development**: `http://localhost:8080`
- **Production**: Configure via environment variable

### API Prefix

All endpoints are prefixed with `/api`

### Headers

Required headers for all authenticated requests:

```typescript
{
  'Authorization': 'Bearer <token>',
  'Content-Type': 'application/json',
  'X-Tenant': '<tenant-slug>' // For multi-tenant endpoints
}
```

### CORS Configuration

The backend allows CORS from:
- `http://localhost:3000` (Next.js dev server)
- `http://localhost:8080`
- Additional origins configurable via `CORS_ALLOWED_ORIGINS`

---

## Authentication

### Authentication Modes

#### 1. H2/Development Mode (Email-based tokens)

For local development, use email-based tokens:

```typescript
// Token format: email address
const token = 'super-admin@test.com';

// Use in Authorization header
headers: {
  'Authorization': `Bearer ${token}`
}
```

**Available Test Users:**
- `super-admin@test.com` - SUPER_ADMIN role
- `parish-admin@test.com` - PARISH_ADMIN role
- `registrar@test.com` - REGISTRAR role
- `priest@test.com` - PRIEST role
- `viewer@test.com` - VIEWER role

#### 2. Production Mode (JWT Tokens)

In production, use real JWT tokens from your OAuth2 provider:

```typescript
// Get token from your auth provider (e.g., Auth0, Google Identity)
const token = await getAccessToken();

headers: {
  'Authorization': `Bearer ${token}`
}
```

### Getting Current User

```typescript
GET /api/admin/users/me
Authorization: Bearer <token>
X-Tenant: <tenant-slug>

Response: UserProfileDto
```

### Role-Based Access

Roles are extracted from JWT and mapped to permissions:
- `SUPER_ADMIN`: Full access
- `PARISH_ADMIN`: Administrative access
- `REGISTRAR`: Create/update records
- `PRIEST`: Create records
- `VIEWER`: Read-only

---

## API Endpoints

### Public Endpoints

#### Get Public Homepage

```http
GET /api/public/{tenantSlug}/home
```

**No authentication required**

**Response:**
```typescript
{
  tenantSlug: string;
  tenantName: string;
  theme: {
    primaryColor?: string;
    secondaryColor?: string;
    logo?: string;
    fontFamily?: string;
    customStyles?: Record<string, any>;
  } | null;
  blocks: ContentBlockDto[];
}
```

---

### Tenant Management

#### List All Tenants

```http
GET /api/admin/tenants
Authorization: Bearer <token>
```

**Required Role:** `SUPER_ADMIN`

**Response:** `TenantDto[]`

#### Get Tenant by ID

```http
GET /api/admin/tenants/{id}
Authorization: Bearer <token>
```

**Required Role:** `SUPER_ADMIN`

**Response:** `TenantDto`

#### Get Tenant by Slug

```http
GET /api/admin/tenants/slug/{slug}
Authorization: Bearer <token>
```

**No role required** (public endpoint)

**Response:** `TenantDto`

#### Create Tenant

```http
POST /api/admin/tenants
Authorization: Bearer <token>
Content-Type: application/json

{
  "slug": string;
  "name": string;
  "parentId": number | null;
  "theme": {
    primaryColor?: string;
    secondaryColor?: string;
    logo?: string;
    fontFamily?: string;
    customStyles?: Record<string, any>;
  } | null;
}
```

**Response:** `TenantDto`

#### Update Tenant

```http
PUT /api/admin/tenants/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": string | null;
  "parentId": number | null;
  "theme": Record<string, any> | null;
}
```

**Required Role:** `SUPER_ADMIN`

**Response:** `TenantDto`

---

### User Management

#### Get Current User Profile

```http
GET /api/admin/users/me
Authorization: Bearer <token>
X-Tenant: <tenant-slug>
```

**Response:** `UserProfileDto`

#### List Users

```http
GET /api/admin/users
Authorization: Bearer <token>
X-Tenant: <tenant-slug>
```

**Required Permission:** `users.manage`

**Response:** `UserDto[]`

#### Invite User

```http
POST /api/admin/users/invite
Authorization: Bearer <token>
X-Tenant: <tenant-slug>
Content-Type: application/json

{
  "email": string;
  "fullName": string;
  "role": "SUPER_ADMIN" | "PARISH_ADMIN" | "REGISTRAR" | "PRIEST" | "VIEWER";
}
```

**Required Permission:** `users.manage`

**Response:** `UserDto`

#### Update User Role

```http
POST /api/admin/users/{id}/role
Authorization: Bearer <token>
X-Tenant: <tenant-slug>
Content-Type: application/json

{
  "role": "SUPER_ADMIN" | "PARISH_ADMIN" | "REGISTRAR" | "PRIEST" | "VIEWER";
}
```

**Required Permission:** `permissions.grant`

**Response:** `MembershipDto`

#### Update User Status

```http
POST /api/admin/users/{id}/status
Authorization: Bearer <token>
X-Tenant: <tenant-slug>
Content-Type: application/json

{
  "status": "ACTIVE" | "INACTIVE";
  "reason": string | null;
}
```

**Required Permission:** `users.manage`

**Response:** `204 No Content`

---

### Sacrament Events

#### List Sacraments

```http
GET /api/sacraments?type={type}&status={status}&fromDate={date}&toDate={date}&cursor={cursor}&limit={limit}
Authorization: Bearer <token>
X-Tenant: <tenant-slug>
```

**Query Parameters:**
- `type`: `BAPTISM` | `CONFIRMATION` | `FIRST_COMMUNION` | `MARRIAGE` | `ORDINATION` | `ANOINTING`
- `status`: `ACTIVE` | `INACTIVE`
- `fromDate`: ISO date string (YYYY-MM-DD)
- `toDate`: ISO date string (YYYY-MM-DD)
- `cursor`: Pagination cursor (ID from previous response)
- `limit`: Page size (default: 20)

**Response:**
```typescript
{
  items: SacramentEventDto[];
  nextCursor: string | null;
  hasMore: boolean;
}
```

**ETag Support:** Include `If-None-Match` header for conditional requests

#### Create Sacrament Event

```http
POST /api/sacraments
Authorization: Bearer <token>
X-Tenant: <tenant-slug>
Idempotency-Key: <unique-key>
Content-Type: application/json

{
  "type": "BAPTISM" | "CONFIRMATION" | "FIRST_COMMUNION" | "MARRIAGE" | "ORDINATION" | "ANOINTING";
  "personId": string; // UUID
  "date": string; // ISO date (YYYY-MM-DD)
  "ministerId": number | null;
  "bookNo": number;
  "pageNo": number;
  "entryNo": number;
}
```

**Required Permission:** `sacraments.create`

**Required Header:** `Idempotency-Key` (unique string for retry safety)

**Response:** `SacramentEventDto`

#### Update Sacrament Event

```http
PUT /api/sacraments/{id}
Authorization: Bearer <token>
X-Tenant: <tenant-slug>
Content-Type: application/json

{
  "personId": string | null; // UUID
  "date": string | null; // ISO date (YYYY-MM-DD)
  "ministerId": number | null;
  "bookNo": number | null;
  "pageNo": number | null;
  "entryNo": number | null;
}
```

**Required Permission:** `sacraments.update`

**Response:** `SacramentEventDto`

#### Update Sacrament Event Status

```http
POST /api/sacraments/{id}/status
Authorization: Bearer <token>
X-Tenant: <tenant-slug>
Content-Type: application/json

{
  "status": "ACTIVE" | "INACTIVE";
  "reason": string | null;
}
```

**Required Permission:** `sacraments.update`

**Response:** `204 No Content`

---

### Certificates

#### Verify Certificate

```http
GET /api/certificates/{serial}/verify
```

**No authentication required** (public endpoint)

**Response:**
```typescript
{
  serialNo: string;
  valid: boolean;
  eventId: number | null;
  issuedAt: string | null; // ISO timestamp
  revocationStatus: "ACTIVE" | "REVOKED" | null;
}
```

---

### Content Management

#### Publish Content Block

```http
POST /api/admin/content/{key}/publish
Authorization: Bearer <token>
X-Tenant: <tenant-slug>
Content-Type: application/json

{
  "content": Record<string, any>;
}
```

**Required Permission:** `settings.edit`

**Response:** `204 No Content`

---

### Audit Logs

#### Query Audit Logs

```http
GET /api/audit?tenantId={id}&actorId={id}&entity={name}&entityId={id}&fromTs={timestamp}&toTs={timestamp}&cursor={cursor}&limit={limit}
Authorization: Bearer <token>
X-Tenant: <tenant-slug>
```

**Required Permission:** `audit.view`

**Query Parameters:**
- `tenantId`: Filter by tenant ID
- `actorId`: Filter by actor (user) ID
- `entity`: Filter by entity type (e.g., "Tenant", "SacramentEvent")
- `entityId`: Filter by entity ID
- `fromTs`: Start timestamp (ISO)
- `toTs`: End timestamp (ISO)
- `cursor`: Pagination cursor
- `limit`: Page size (default: 20)

**Response:**
```typescript
{
  items: AuditLogDto[];
  nextCursor: string | null;
  hasMore: boolean;
}
```

---

## Data Transfer Objects (DTOs)

### TenantDto

```typescript
interface TenantDto {
  id: number;
  slug: string;
  name: string;
  parentId: number | null;
  theme: {
    primaryColor?: string;
    secondaryColor?: string;
    logo?: string;
    fontFamily?: string;
    customStyles?: Record<string, any>;
  } | null;
  createdAt: string; // ISO timestamp
}
```

### CreateTenantRequest

```typescript
interface CreateTenantRequest {
  slug: string;
  name: string;
  parentId?: number | null;
  theme?: Record<string, any> | null;
}
```

### UpdateTenantRequest

```typescript
interface UpdateTenantRequest {
  name?: string | null;
  parentId?: number | null;
  theme?: Record<string, any> | null;
}
```

### UserDto

```typescript
interface UserDto {
  id: number;
  email: string;
  fullName: string;
  mfaEnabled: boolean;
  status: "ACTIVE" | "INACTIVE";
  createdAt: string; // ISO timestamp
}
```

### UserProfileDto

```typescript
interface UserProfileDto {
  id: number;
  email: string;
  fullName: string;
  mfaEnabled: boolean;
  memberships: MembershipDto[];
  permissions: string[];
}
```

### MembershipDto

```typescript
interface MembershipDto {
  tenantId: number;
  role: "SUPER_ADMIN" | "PARISH_ADMIN" | "REGISTRAR" | "PRIEST" | "VIEWER";
  status: "ACTIVE" | "INACTIVE";
}
```

### InviteUserRequest

```typescript
interface InviteUserRequest {
  email: string;
  fullName: string;
  role: "SUPER_ADMIN" | "PARISH_ADMIN" | "REGISTRAR" | "PRIEST" | "VIEWER";
}
```

### UpdateUserRoleRequest

```typescript
interface UpdateUserRoleRequest {
  role: "SUPER_ADMIN" | "PARISH_ADMIN" | "REGISTRAR" | "PRIEST" | "VIEWER";
}
```

### UpdateUserStatusRequest

```typescript
interface UpdateUserStatusRequest {
  status: "ACTIVE" | "INACTIVE";
  reason?: string | null;
}
```

### SacramentEventDto

```typescript
interface SacramentEventDto {
  id: number;
  tenantId: number;
  type: "BAPTISM" | "CONFIRMATION" | "FIRST_COMMUNION" | "MARRIAGE" | "ORDINATION" | "ANOINTING";
  personId: string; // UUID
  date: string; // ISO date (YYYY-MM-DD)
  ministerId: number | null;
  bookNo: number;
  pageNo: number;
  entryNo: number;
  status: "ACTIVE" | "INACTIVE";
  createdAt: string; // ISO timestamp
  updatedAt: string | null; // ISO timestamp
}
```

### CreateSacramentEventRequest

```typescript
interface CreateSacramentEventRequest {
  type: "BAPTISM" | "CONFIRMATION" | "FIRST_COMMUNION" | "MARRIAGE" | "ORDINATION" | "ANOINTING";
  personId: string; // UUID
  date: string; // ISO date (YYYY-MM-DD)
  ministerId?: number | null;
  bookNo: number;
  pageNo: number;
  entryNo: number;
}
```

### UpdateSacramentEventRequest

```typescript
interface UpdateSacramentEventRequest {
  personId?: string | null; // UUID
  date?: string | null; // ISO date (YYYY-MM-DD)
  ministerId?: number | null;
  bookNo?: number | null;
  pageNo?: number | null;
  entryNo?: number | null;
}
```

### UpdateSacramentEventStatusRequest

```typescript
interface UpdateSacramentEventStatusRequest {
  status: "ACTIVE" | "INACTIVE";
  reason?: string | null;
}
```

### CertificateVerificationDto

```typescript
interface CertificateVerificationDto {
  serialNo: string;
  valid: boolean;
  eventId: number | null;
  issuedAt: string | null; // ISO timestamp
  revocationStatus: "ACTIVE" | "REVOKED" | null;
}
```

### ContentBlockDto

```typescript
interface ContentBlockDto {
  id: number;
  tenantId: number;
  key: string;
  published: Record<string, any> | null;
  updatedAt: string; // ISO timestamp
}
```

### PublishContentBlockRequest

```typescript
interface PublishContentBlockRequest {
  content: Record<string, any>;
}
```

### HomePageDto

```typescript
interface HomePageDto {
  tenantSlug: string;
  tenantName: string;
  theme: {
    primaryColor?: string;
    secondaryColor?: string;
    logo?: string;
    fontFamily?: string;
    customStyles?: Record<string, any>;
  } | null;
  blocks: ContentBlockDto[];
}
```

### AuditLogDto

```typescript
interface AuditLogDto {
  id: number;
  tenantId: number | null;
  actorId: number | null;
  action: string;
  entity: string;
  entityId: string | null;
  before: Record<string, any> | null;
  after: Record<string, any> | null;
  ts: string; // ISO timestamp
  hash: string | null;
}
```

### CursorPage<T>

```typescript
interface CursorPage<T> {
  items: T[];
  nextCursor: string | null;
  hasMore: boolean;
}
```

---

## Error Handling

### Error Response Format

All errors follow RFC 7807 Problem Details format:

```typescript
interface ProblemDetail {
  type: string; // URI reference
  title: string; // Short summary
  status: number; // HTTP status code
  detail: string; // Detailed message
  instance: string; // URI reference to the specific occurrence
  code: string; // Application-specific error code
  timestamp: string; // ISO timestamp
}
```

### Common HTTP Status Codes

- `200 OK`: Success
- `201 Created`: Resource created
- `204 No Content`: Success with no response body
- `400 Bad Request`: Invalid request data
- `401 Unauthorized`: Missing or invalid authentication
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `409 Conflict`: Duplicate resource (e.g., idempotency key already used)
- `422 Unprocessable Entity`: Validation error
- `500 Internal Server Error`: Server error

### Error Handling Example

```typescript
try {
  const response = await fetch('/api/admin/tenants', {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });

  if (!response.ok) {
    const error: ProblemDetail = await response.json();
    throw new Error(error.detail || error.title);
  }

  const data = await response.json();
  return data;
} catch (error) {
  console.error('API Error:', error);
  throw error;
}
```

---

## TypeScript Types

### Complete Type Definitions

Create a `types/api.ts` file:

```typescript
// Enums
export type Role = 'SUPER_ADMIN' | 'PARISH_ADMIN' | 'REGISTRAR' | 'PRIEST' | 'VIEWER';
export type Status = 'ACTIVE' | 'INACTIVE';
export type SacramentType = 'BAPTISM' | 'CONFIRMATION' | 'FIRST_COMMUNION' | 'MARRIAGE' | 'ORDINATION' | 'ANOINTING';
export type RevocationStatus = 'ACTIVE' | 'REVOKED';

// DTOs (include all from Data Transfer Objects section above)
// ... (copy all interfaces from DTOs section)

// API Response Types
export interface ApiResponse<T> {
  data: T;
}

export interface CursorPage<T> {
  items: T[];
  nextCursor: string | null;
  hasMore: boolean;
}

// Error Types
export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  code: string;
  timestamp: string;
}
```

---

## Next.js Integration Guide

### 1. Environment Variables

Create `.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_API_BASE=/api
```

### 2. API Client Setup

Create `lib/api-client.ts`:

```typescript
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
const API_PREFIX = process.env.NEXT_PUBLIC_API_BASE || '/api';

export class ApiClient {
  private baseUrl: string;
  private token: string | null = null;
  private tenantSlug: string | null = null;

  constructor() {
    this.baseUrl = `${API_BASE_URL}${API_PREFIX}`;
  }

  setToken(token: string) {
    this.token = token;
  }

  setTenant(tenantSlug: string) {
    this.tenantSlug = tenantSlug;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`;
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }

    if (this.tenantSlug) {
      headers['X-Tenant'] = this.tenantSlug;
    }

    const response = await fetch(url, {
      ...options,
      headers,
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({
        title: 'Error',
        detail: response.statusText,
        status: response.status,
      }));
      throw new Error(error.detail || error.title);
    }

    // Handle 204 No Content
    if (response.status === 204) {
      return null as T;
    }

    return response.json();
  }

  // Tenant endpoints
  async getTenants(): Promise<TenantDto[]> {
    return this.request<TenantDto[]>('/admin/tenants');
  }

  async getTenantById(id: number): Promise<TenantDto> {
    return this.request<TenantDto>(`/admin/tenants/${id}`);
  }

  async getTenantBySlug(slug: string): Promise<TenantDto> {
    return this.request<TenantDto>(`/admin/tenants/slug/${slug}`);
  }

  async createTenant(data: CreateTenantRequest): Promise<TenantDto> {
    return this.request<TenantDto>('/admin/tenants', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async updateTenant(id: number, data: UpdateTenantRequest): Promise<TenantDto> {
    return this.request<TenantDto>(`/admin/tenants/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  // User endpoints
  async getCurrentUser(): Promise<UserProfileDto> {
    return this.request<UserProfileDto>('/admin/users/me');
  }

  async getUsers(): Promise<UserDto[]> {
    return this.request<UserDto[]>('/admin/users');
  }

  async inviteUser(data: InviteUserRequest): Promise<UserDto> {
    return this.request<UserDto>('/admin/users/invite', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async updateUserRole(userId: number, role: Role): Promise<MembershipDto> {
    return this.request<MembershipDto>(`/admin/users/${userId}/role`, {
      method: 'POST',
      body: JSON.stringify({ role }),
    });
  }

  async updateUserStatus(
    userId: number,
    status: Status,
    reason?: string
  ): Promise<void> {
    return this.request<void>(`/admin/users/${userId}/status`, {
      method: 'POST',
      body: JSON.stringify({ status, reason }),
    });
  }

  // Sacrament endpoints
  async getSacraments(params?: {
    type?: SacramentType;
    status?: Status;
    fromDate?: string;
    toDate?: string;
    cursor?: string;
    limit?: number;
  }): Promise<CursorPage<SacramentEventDto>> {
    const queryParams = new URLSearchParams();
    if (params?.type) queryParams.append('type', params.type);
    if (params?.status) queryParams.append('status', params.status);
    if (params?.fromDate) queryParams.append('fromDate', params.fromDate);
    if (params?.toDate) queryParams.append('toDate', params.toDate);
    if (params?.cursor) queryParams.append('cursor', params.cursor);
    if (params?.limit) queryParams.append('limit', params.limit.toString());

    const query = queryParams.toString();
    return this.request<CursorPage<SacramentEventDto>>(
      `/sacraments${query ? `?${query}` : ''}`
    );
  }

  async createSacrament(
    data: CreateSacramentEventRequest,
    idempotencyKey: string
  ): Promise<SacramentEventDto> {
    return this.request<SacramentEventDto>('/sacraments', {
      method: 'POST',
      headers: {
        'Idempotency-Key': idempotencyKey,
      },
      body: JSON.stringify(data),
    });
  }

  async updateSacrament(
    id: number,
    data: UpdateSacramentEventRequest
  ): Promise<SacramentEventDto> {
    return this.request<SacramentEventDto>(`/sacraments/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async updateSacramentStatus(
    id: number,
    status: Status,
    reason?: string
  ): Promise<void> {
    return this.request<void>(`/sacraments/${id}/status`, {
      method: 'POST',
      body: JSON.stringify({ status, reason }),
    });
  }

  // Certificate endpoints
  async verifyCertificate(serialNo: string): Promise<CertificateVerificationDto> {
    return this.request<CertificateVerificationDto>(
      `/certificates/${serialNo}/verify`
    );
  }

  // Public endpoints
  async getPublicHome(tenantSlug: string): Promise<HomePageDto> {
    return this.request<HomePageDto>(`/public/${tenantSlug}/home`);
  }
}

export const apiClient = new ApiClient();
```

### 3. Authentication Context

Create `contexts/AuthContext.tsx`:

```typescript
'use client';

import { createContext, useContext, useState, useEffect } from 'react';
import { apiClient } from '@/lib/api-client';
import { UserProfileDto } from '@/types/api';

interface AuthContextType {
  user: UserProfileDto | null;
  token: string | null;
  tenantSlug: string | null;
  login: (token: string) => void;
  logout: () => void;
  setTenant: (slug: string) => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [tenantSlug, setTenantSlug] = useState<string | null>(null);
  const [user, setUser] = useState<UserProfileDto | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Load token from localStorage
    const savedToken = localStorage.getItem('auth_token');
    const savedTenant = localStorage.getItem('tenant_slug');
    
    if (savedToken) {
      setToken(savedToken);
      apiClient.setToken(savedToken);
    }
    
    if (savedTenant) {
      setTenantSlug(savedTenant);
      apiClient.setTenant(savedTenant);
    }
    
    setLoading(false);
  }, []);

  useEffect(() => {
    if (token) {
      // Fetch user profile
      apiClient.getCurrentUser()
        .then(setUser)
        .catch(() => {
          // Token invalid, clear auth
          logout();
        });
    } else {
      setUser(null);
    }
  }, [token, tenantSlug]);

  const login = (newToken: string) => {
    setToken(newToken);
    apiClient.setToken(newToken);
    localStorage.setItem('auth_token', newToken);
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    apiClient.setToken('');
    localStorage.removeItem('auth_token');
  };

  const setTenant = (slug: string) => {
    setTenantSlug(slug);
    apiClient.setTenant(slug);
    localStorage.setItem('tenant_slug', slug);
  };

  return (
    <AuthContext.Provider
      value={{ user, token, tenantSlug, login, logout, setTenant, loading }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
```

### 4. Example Page Component

```typescript
'use client';

import { useEffect, useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { apiClient } from '@/lib/api-client';
import { TenantDto } from '@/types/api';

export default function TenantsPage() {
  const { token, tenantSlug } = useAuth();
  const [tenants, setTenants] = useState<TenantDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (token) {
      apiClient.getTenants()
        .then(setTenants)
        .catch(console.error)
        .finally(() => setLoading(false));
    }
  }, [token]);

  if (loading) return <div>Loading...</div>;

  return (
    <div>
      <h1>Tenants</h1>
      <ul>
        {tenants.map((tenant) => (
          <li key={tenant.id}>
            {tenant.name} ({tenant.slug})
          </li>
        ))}
      </ul>
    </div>
  );
}
```

---

## Example Code

### React Hook for Data Fetching

```typescript
import { useState, useEffect } from 'react';

export function useApi<T>(
  fetchFn: () => Promise<T>,
  deps: any[] = []
) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    
    fetchFn()
      .then(setData)
      .catch(setError)
      .finally(() => setLoading(false));
  }, deps);

  return { data, loading, error };
}
```

### Usage Example

```typescript
function TenantsList() {
  const { token } = useAuth();
  const { data: tenants, loading, error } = useApi(
    () => apiClient.getTenants(),
    [token]
  );

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;

  return (
    <ul>
      {tenants?.map((tenant) => (
        <li key={tenant.id}>{tenant.name}</li>
      ))}
    </ul>
  );
}
```

---

## Notes for Frontend Development

1. **Idempotency Keys**: Always generate unique idempotency keys for POST requests to `/api/sacraments`. Use UUIDs or timestamp-based keys.

2. **Cursor Pagination**: Use the `nextCursor` from response to fetch next page. Set `hasMore` to false when `nextCursor` is null.

3. **ETag Support**: Include `If-None-Match` header for GET requests to `/api/sacraments` to enable conditional requests and reduce bandwidth.

4. **Error Handling**: Always handle API errors gracefully. Check for `401` to redirect to login, `403` to show permission denied message.

5. **Tenant Context**: Always set `X-Tenant` header for multi-tenant endpoints. Store tenant selection in user preferences.

6. **Token Management**: Store JWT tokens securely (httpOnly cookies in production, localStorage for dev). Implement token refresh if your OAuth provider supports it.

7. **Type Safety**: Use TypeScript strictly. All DTOs should be typed to catch errors at compile time.

---

## Updates Log

This document will be updated as the API evolves. Check this section for recent changes:

- **2025-11-21**: Initial documentation created
- **2025-11-21**: Added `/api` prefix to all endpoints
- **2025-11-21**: Documented H2 authentication mode with email-based tokens

