import * as React from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useEffect, useMemo, useState } from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { Search, Filter, X, ChevronLeft, ChevronRight, AlertCircle, Package } from 'lucide-react'
import { cn } from '@/shared/lib/utils'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { resolveDemoImageUrl } from '@/shared/lib/demoMedia'
import {
  fetchClientCategories,
  fetchClientResources,
  isClientCatalogApiError,
  type ClientCategoryDto,
} from '@/shared/lib/clientCatalogApi'

const chipVariants = cva(
  'inline-flex items-center justify-center rounded-full border text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        default:
          'border-transparent bg-primary text-primary-foreground hover:bg-primary/80 focus-visible:ring-ring shadow-sm/2',
        secondary:
          'border-transparent bg-secondary text-secondary-foreground hover:bg-secondary/80 focus-visible:ring-ring',
        destructive:
          'border-transparent bg-destructive text-destructive-foreground hover:bg-destructive/80 focus-visible:ring-destructive shadow-sm/2',
        outline:
          'border-border text-foreground hover:bg-accent hover:text-accent-foreground focus-visible:ring-ring shadow-sm/2',
        ghost:
          'border-transparent text-foreground hover:bg-accent hover:text-accent-foreground focus-visible:ring-ring',
      },
      size: {
        sm: 'h-6 px-2 gap-1 text-sm',
        default: 'h-7 px-3 gap-1.5 text-sm',
        lg: 'h-8 px-4 text-sm gap-2',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
)

interface ChipProps extends React.HTMLAttributes<HTMLDivElement>, VariantProps<typeof chipVariants> {
  dismissible?: boolean
  onDismiss?: () => void
}

const Chip = React.forwardRef<HTMLDivElement, ChipProps>(
  ({ className, variant, size, dismissible = false, onDismiss, children, ...props }, ref) => {
    const closeIconSize = size === 'sm' ? 10 : size === 'lg' ? 12 : 10

    const handleDismiss = (e: React.MouseEvent) => {
      e.stopPropagation()
      onDismiss?.()
    }

    return (
      <div ref={ref} className={cn(chipVariants({ variant, size }), className)} {...props}>
        {children}
        {dismissible && (
          <button
            type="button"
            onClick={handleDismiss}
            className="shrink-0 rounded-full p-0.5 transition-colors hover:bg-black/10 dark:hover:bg-white/10"
            aria-label="Remove"
          >
            <X size={closeIconSize} />
          </button>
        )}
      </div>
    )
  },
)

Chip.displayName = 'Chip'

function Empty({ className, ...props }: React.ComponentProps<'div'>) {
  return (
    <div
      className={cn(
        'flex min-w-0 flex-1 flex-col items-center justify-center gap-6 rounded-xl border border-dashed p-6 text-center text-balance md:p-12',
        className,
      )}
      {...props}
    />
  )
}

function EmptyHeader({ className, ...props }: React.ComponentProps<'div'>) {
  return <div className={cn('flex max-w-sm flex-col items-center text-center', className)} {...props} />
}

function EmptyMedia({ className, ...props }: React.ComponentProps<'div'>) {
  return (
    <div
      className={cn('relative mb-6 flex size-16 items-center justify-center rounded-full bg-muted', className)}
      {...props}
    />
  )
}

function EmptyTitle({ className, ...props }: React.ComponentProps<'div'>) {
  return <div className={cn('font-heading text-xl leading-none font-semibold', className)} {...props} />
}

function EmptyDescription({ className, ...props }: React.ComponentProps<'p'>) {
  return <p className={cn('mt-2 text-sm/relaxed text-muted-foreground', className)} {...props} />
}

interface FilterOption {
  id: string
  name: string
  value: string
}

interface SearchResult {
  id: string
  title: string
  description: string
  category: string
  price?: string
  image?: string
  tags?: string[]
}

interface SearchPageProps {
  className?: string
}

export function SearchPage({ className }: SearchPageProps) {
  const [searchParams, setSearchParams] = useSearchParams()
  const [searchQuery, setSearchQuery] = useState(searchParams.get('q') ?? '')
  const [selectedCategory, setSelectedCategory] = useState(searchParams.get('category') ?? 'all')
  const [sortBy, setSortBy] = useState<'relevance' | 'priceAsc' | 'priceDesc' | 'rating'>(
    (searchParams.get('sort') as 'relevance' | 'priceAsc' | 'priceDesc' | 'rating') ?? 'relevance',
  )
  const [currentPage, setCurrentPage] = useState(Number(searchParams.get('page') ?? '1') || 1)
  const [isLoading, setIsLoading] = useState(false)
  const [hasError, setHasError] = useState(false)
  const [results, setResults] = useState<SearchResult[]>([])
  const [categories, setCategories] = useState<ClientCategoryDto[]>([])
  const [totalResults, setTotalResults] = useState(0)
  const [showFilters, setShowFilters] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  const itemsPerPage = 6
  const totalPages = Math.max(1, Math.ceil(totalResults / itemsPerPage))

  useEffect(() => {
    const params = new URLSearchParams()
    if (searchQuery.trim()) params.set('q', searchQuery.trim())
    if (selectedCategory !== 'all') params.set('category', selectedCategory)
    if (sortBy !== 'relevance') params.set('sort', sortBy)
    if (currentPage > 1) params.set('page', String(currentPage))
    setSearchParams(params, { replace: true })
  }, [searchQuery, selectedCategory, sortBy, currentPage, setSearchParams])

  useEffect(() => {
    let active = true
    const timeout = setTimeout(async () => {
      setIsLoading(true)
      setHasError(false)
      try {
        const [resourcePage, categoryList] = await Promise.all([
          fetchClientResources({
            q: searchQuery.trim(),
            category: selectedCategory,
            sort: sortBy,
            page: currentPage - 1,
            size: itemsPerPage,
          }),
          fetchClientCategories(),
        ])

        if (!active) return

        setResults(
          resourcePage.items.map((item) => ({
            id: item.id,
            title: item.name,
            description: item.merchantName || item.category,
            category: item.category,
            price: `$${item.price.toFixed(2)}`,
            image: resolveDemoImageUrl(item.imageUrl),
            tags: [item.category, item.resourceType ?? 'service'],
          })),
        )
        setTotalResults(resourcePage.total)
        setCategories(categoryList)
      } catch (error) {
        if (!active) return
        setHasError(true)
        if (!isClientCatalogApiError(error)) {
          // Keep behavior consistent for unexpected runtime errors.
          console.error(error)
        }
      } finally {
        if (active) setIsLoading(false)
      }
    }, 300)

    return () => {
      active = false
      clearTimeout(timeout)
    }
  }, [searchQuery, selectedCategory, sortBy, currentPage, reloadKey])

  const activeFilters = useMemo<FilterOption[]>(() => {
    const filters: FilterOption[] = []
    if (selectedCategory !== 'all') {
      const category = categories.find((item) => item.key === selectedCategory)
      filters.push({
        id: 'category',
        name: 'Category',
        value: category?.label ?? selectedCategory,
      })
    }
    if (sortBy !== 'relevance') {
      const sortLabelMap: Record<string, string> = {
        priceAsc: 'Price: Low to High',
        priceDesc: 'Price: High to Low',
        rating: 'Rating',
      }
      filters.push({
        id: 'sort',
        name: 'Sort',
        value: sortLabelMap[sortBy] ?? sortBy,
      })
    }
    return filters
  }, [categories, selectedCategory, sortBy])

  const removeFilter = (id: string) => {
    if (id === 'category') setSelectedCategory('all')
    if (id === 'sort') setSortBy('relevance')
    setCurrentPage(1)
  }

  const clearAllFilters = () => {
    setSearchQuery('')
    setSelectedCategory('all')
    setSortBy('relevance')
    setCurrentPage(1)
  }

  const handlePageChange = (page: number) => {
    setCurrentPage(page)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleSearch = (value: string) => {
    setSearchQuery(value)
    setCurrentPage(1)
  }

  return (
    <div className={cn('min-h-screen bg-background', className)}>
      <div className="sticky top-16 z-40 border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container mx-auto px-4 py-3 md:py-4">
          <div className="flex flex-col gap-4">
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  type="search"
                  placeholder="Search products..."
                  value={searchQuery}
                  onChange={(e) => handleSearch(e.target.value)}
                  className="pl-9 pr-4"
                />
              </div>
              <Button variant="outline" size="icon" className="shrink-0" onClick={() => setShowFilters((v) => !v)}>
                <Filter className="h-4 w-4" />
              </Button>
            </div>

            {showFilters && (
              <div className="flex flex-wrap gap-2">
                <Select
                  value={selectedCategory}
                  onValueChange={(value) => {
                    setSelectedCategory(value)
                    setCurrentPage(1)
                  }}
                >
                  <SelectTrigger className="h-9 w-[220px]">
                    <SelectValue placeholder="Category" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All categories</SelectItem>
                    {categories.map((category) => (
                      <SelectItem key={category.key} value={category.key}>
                        {category.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            {activeFilters.length > 0 && (
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-sm font-medium text-muted-foreground">Filters:</span>
                {activeFilters.map((filter) => (
                  <Chip
                    key={filter.id}
                    variant="secondary"
                    size="sm"
                    dismissible
                    onDismiss={() => removeFilter(filter.id)}
                  >
                    {filter.name}: {filter.value}
                  </Chip>
                ))}
                <Button variant="ghost" size="sm" onClick={clearAllFilters} className="h-6 px-2 text-xs">
                  Clear all
                </Button>
              </div>
            )}

            <div className="flex items-center justify-between gap-4">
              <p className="text-sm text-muted-foreground">{totalResults} results found</p>
              <Select
                value={sortBy}
                onValueChange={(value) => {
                  setSortBy(value as 'relevance' | 'priceAsc' | 'priceDesc' | 'rating')
                  setCurrentPage(1)
                }}
              >
                <SelectTrigger className="h-9 w-[180px]">
                  <SelectValue placeholder="Sort by" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="relevance">Relevance</SelectItem>
                  <SelectItem value="priceAsc">Price: Low to High</SelectItem>
                  <SelectItem value="priceDesc">Price: High to Low</SelectItem>
                  <SelectItem value="rating">Rating</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </div>
      </div>

      <div className="container mx-auto px-4 py-6 md:py-8">
        {isLoading && (
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <Card key={i} className="overflow-hidden">
                <Skeleton className="h-48 w-full" />
                <CardHeader>
                  <Skeleton className="h-6 w-3/4" />
                  <Skeleton className="mt-2 h-4 w-full" />
                </CardHeader>
                <CardContent>
                  <Skeleton className="h-4 w-1/2" />
                </CardContent>
              </Card>
            ))}
          </div>
        )}

        {hasError && !isLoading && (
          <Empty>
            <EmptyHeader>
              <EmptyMedia>
                <AlertCircle className="h-8 w-8 text-destructive" />
              </EmptyMedia>
              <EmptyTitle>Something went wrong</EmptyTitle>
              <EmptyDescription>We couldn't load the results. Please try again later.</EmptyDescription>
            </EmptyHeader>
            <Button
              onClick={() => {
                setHasError(false)
                setReloadKey((k) => k + 1)
              }}
              className="mt-4"
            >
              Try Again
            </Button>
          </Empty>
        )}

        {!isLoading && !hasError && results.length === 0 && (
          <Empty>
            <EmptyHeader>
              <EmptyMedia>
                <Package className="h-8 w-8 text-muted-foreground" />
              </EmptyMedia>
              <EmptyTitle>No results found</EmptyTitle>
              <EmptyDescription>Try adjusting your search or filters to find what you're looking for.</EmptyDescription>
            </EmptyHeader>
            <Button onClick={clearAllFilters} variant="outline" className="mt-4">
              Clear Filters
            </Button>
          </Empty>
        )}

        {!isLoading && !hasError && results.length > 0 && (
          <>
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
              {results.map((result) => (
                <Card
                  key={result.id}
                  className="group overflow-hidden transition-shadow duration-300 hover:shadow-lg"
                >
                  {result.image && (
                    <div className="relative h-48 overflow-hidden bg-muted">
                      <img
                        src={result.image}
                        alt={result.title}
                        className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                      />
                    </div>
                  )}
                  <CardHeader>
                    <div className="flex items-start justify-between gap-2">
                      <CardTitle className="line-clamp-1 text-lg">{result.title}</CardTitle>
                      {result.price && (
                        <Badge variant="secondary" className="shrink-0">
                          {result.price}
                        </Badge>
                      )}
                    </div>
                    <CardDescription className="line-clamp-2">{result.description}</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="flex flex-wrap gap-1">
                      {result.tags?.map((tag) => (
                        <Badge key={tag} variant="outline" className="text-xs">
                          {tag}
                        </Badge>
                      ))}
                    </div>
                  </CardContent>
                  <CardFooter>
                    <Button asChild className="w-full" variant="outline">
                      <Link to={`/booking/${result.id}`}>View Details</Link>
                    </Button>
                  </CardFooter>
                </Card>
              ))}
            </div>

            {totalPages > 1 && (
              <div className="mt-12 flex items-center justify-center gap-2">
                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 1}
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>

                <div className="flex items-center gap-1">
                  {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => {
                    if (
                      page === 1 ||
                      page === totalPages ||
                      (page >= currentPage - 1 && page <= currentPage + 1)
                    ) {
                      return (
                        <Button
                          key={page}
                          variant={currentPage === page ? 'default' : 'outline'}
                          size="icon"
                          onClick={() => handlePageChange(page)}
                          className="h-9 w-9"
                        >
                          {page}
                        </Button>
                      )
                    }
                    if (page === currentPage - 2 || page === currentPage + 2) {
                      return (
                        <span key={page} className="px-2 text-muted-foreground">
                          ...
                        </span>
                      )
                    }
                    return null
                  })}
                </div>

                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage === totalPages}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
