import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor — auth token will be attached here in a later phase
apiClient.interceptors.request.use((config) => {
  return config
})

// Response interceptor — centralised error handling will be added here
apiClient.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error),
)

export default apiClient
