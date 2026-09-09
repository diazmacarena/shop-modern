import {Component, Input, OnInit} from '@angular/core';

@Component({
    standalone: false,
  selector: 'app-pagination',
  templateUrl: './pagination.component.html',
  styleUrls: ['./pagination.component.css']
})
export class PaginationComponent implements OnInit {

  @Input() currentPage: any;

  constructor() {
  }

  ngOnInit() {
  }

  counter(i = 1) {
    return new Array(i);
  }
}
