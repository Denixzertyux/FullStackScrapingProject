import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { ProductService, Product } from '../../services/product.service';


@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule], 
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.scss']
})

export class DashboardComponent implements OnInit {

  products: Product[] = [];
  searchTerm: string = '';
  
 
  editingProductId: number | null = null;
  editedProduct: Partial<Product> = {};

  
  selectedFile: File | null = null;
  isUploading: boolean = false;

  constructor(private productService: ProductService,private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  // --- 1. Încărcarea și Filtrarea Datelor ---
  
loadProducts(): void {
    console.log('1. Cerere trimisă! searchTerm este: "' + this.searchTerm + '"');
    this.productService.getProducts(this.searchTerm).subscribe({
      next: (data: Product[]) => { 
        console.log('2. Date primite de la server:', data);
        this.products = data;
        this.cdr.detectChanges();
      },
      error: (err: any) => console.error('Eroare la preluarea produselor', err) // <-- Adăugat : any
    });
  }


  onSearch(): void {
    this.loadProducts();
  }

  // --- 2. Editarea ---

  startEditing(product: Product): void {
    this.editingProductId = product.id;
    this.editedProduct = { ...product }; 
  }

  cancelEditing(): void {
    this.editingProductId = null;
    this.editedProduct = {};
  }

saveEdit(): void {
    if (this.editingProductId && this.editedProduct) {
      this.productService.updateProduct(this.editingProductId, this.editedProduct as Product).subscribe({
        next: (data: Product) => { 
          this.loadProducts(); 
          this.cancelEditing();
        },
        error: (err: any) => console.error('Eroare la actualizare', err) // <-- Adăugat : any
      });
    }
  }

  // --- 3. Ștergerea ---

  deleteProduct(id: number): void {
    if (confirm('Ești sigur că vrei să ștergi acest produs?')) {
      this.productService.deleteProduct(id).subscribe({
        next: () => this.loadProducts(),
        error: (err) => console.error('Eroare la ștergere', err)
      });
    }
  }

  // --- 4. Upload PDF ---

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file && file.type === 'application/pdf') {
      this.selectedFile = file;
    } else {
      alert('Te rog să selectezi un fișier PDF valid.');
      this.selectedFile = null;
    }
  }

uploadPdf(): void {
    if (!this.selectedFile) return;

    this.isUploading = true;
    this.productService.uploadPdf(this.selectedFile).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'produse_extrase.csv';
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        
        this.isUploading = false;
        this.selectedFile = null;
        this.cdr.detectChanges();
        alert('Fișierul CSV a fost generat și descărcat cu succes!');
      },
      error: (err: any) => { // <-- Adăugat : any
        console.error('Eroare la upload', err);
        this.isUploading = false;
        this.cdr.detectChanges();
        alert('A apărut o eroare la procesarea fișierului PDF.');
      }
    });
  }
}